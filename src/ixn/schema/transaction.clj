(ns ixn.schema.transaction
  (:require
   [clojure.tools.logging :as log]
   [xtdb.api :as xtdb]
   [malli.core :as m]
   [malli.generator :as mg]
   [ixn.db :refer [xtdb-node transact!]]
   [ixn.utils :refer [uuid now]]
   [ixn.money :refer [rounded Money]]
   [ixn.schema.account :refer [AccountNumber]]
   [ixn.schema.journal :refer [JournalType]]
   [ixn.schema.core :refer [NotEmptyString]]))


(def TransactionLine
  [:map
   {:closed true
    :title  {:en "Transaction line" :nl "Transactie regel"}}
   [:transaction/id uuid?]
   [:transaction/line pos-int?]
   [:transaction/account AccountNumber]
   [:transaction/description NotEmptyString]
   [:transaction/journal JournalType]
   [:transaction/year pos-int?]
   [:transacttion/period pos-int?]
   [:transaction/date inst?]
   [:transaction/amount Money]
   [:transaction/sub-admin string?]
   [:transaction/cost-center string?]
   [:transaction/side [:enum {} :debit :credit]]])

(comment
  (mg/generate Money))

(defn- line-totals [s line]
  (let [{:keys [:transaction/amount :transaction/side]} line]
    (if (= :debit side)
      [(+ (first s) amount) (last s)]
      [(first s) (+ (last s) amount)])))

(defn- balance [lines]
  (let [[d c] (reduce line-totals [0.0 0.0] lines)]
    (= 0.0 (- d c))))

(def Transaction
  [:and
   [:vector TransactionLine]])
;[:= 0.0 [:fn (balance [:vector TransactionLine])]]])

(def SalesBooking
  [:map
   [:invoice-date inst?]
   [:description string?]
   [:debtor-id string?]
   [:amount number?]
   [:account AccountNumber]
   [:turnover-account AccountNumber]
   [:vat [:and [:and
                decimal?
                [:>= 3.0]
                [:<= 25.0]]]]])

(defn- fetch-account
  "Fetch an account by id"
  [id]
  (let [res (xtdb/q
             (xtdb/db xtdb-node)
             '{:find     [?act ?id]
               :where    [[?act :account/id ?id]
                          [?act :account/summary-level 0]]
               :in       [?id]
               :order-by [[?id :asc]]}
             id)]
    (if (= 1 (count res))
      (ffirst res)
      (throw (str "Unknown account number " id)))))

(comment
  (fetch-account "80100"))

(defn- book-sales-invoice
  [{:keys [:invoice-date :description :debtor-id :amount :turnover-account :vat]}]
  (let [id (uuid)
        invoice-amt (rounded amount)
        turnover-amt (rounded (/ invoice-amt (+ 1.0 (/ vat 100.0))))
        vat-amt (- invoice-amt turnover-amt)
        ;; to-account (fetch-account turnover-account)
        debtors-account (fetch-account "12100")
        vat-account-high (fetch-account "27030")]
        ;; vat-account-low (fetch-account "27040")

    [{:transaction/id          id
      :transaction/line 1
      :transaction/date        invoice-date
      :transaction/account     debtors-account
      :transaction/description description
      :transaction/cost-center ""
      :transaction/sub-admin   debtor-id
      :transaction/amount      invoice-amt
      :transaction/side        :debit}

     {:transaction/id          id
      :transaction/line 2
      :transaction/date        invoice-date
      :transaction/account     turnover-account
      :transaction/description description
      :transaction/cost-center ""
      :transaction/sub-admin   debtor-id
      :transaction/amount      turnover-amt
      :transaction/side        :credit}

     {:transaction/id          id
      :transaction/line 3
      :transaction/date        invoice-date
      :transaction/account     vat-account-high
      :transaction/description description
      :transaction/cost-center ""
      :transaction/sub-admin   debtor-id
      :transaction/amount      vat-amt
      :transaction/side        :credit}]))

(defn transact-sales-invoice!
  [params]
  (if (balance (book-sales-invoice params))
    (transact! (book-sales-invoice params))
    (log/error (str "Sales invoice balance for booking " params " is not 0.00"))))


(def PurchaseBooking
  [:map
   [:invoice-date inst?]
   [:description string?]
   [:creditor-id string?]
   [:amount number?]
   [:turnover-account [:re
                       {:error-message {:en "Only 5 digit accounts accept bookings."
                                        :nl "Alleen 5 cijferige rekeningen accepteren een boeking."}}
                       #"^[0-9]{5}$"]]
   [:vat [number? {:min 3.0 :max 25.0}]]])

(defn book-purchase-invoice
  [{:keys [:invoice-date :description :creditor-id :amount :turnover-account :vat]}]
  (let [id (uuid)]
    [{:transaction/id          id
      :transaction/line 1
      :transaction/date        invoice-date
      :transaction/account     "12100"
      :transaction/description description
      :transaction/cost-center ""
      :transaction/sub-admin   creditor-id
      :transaction/amount      (rounded 2 amount)
      :transaction/side        :credit}
     {:transaction/id          id
      :transaction/line 2
      :transaction/date        invoice-date
      :transaction/account     turnover-account
      :transaction/description description
      :transaction/cost-center ""
      :transaction/sub-admin   creditor-id
      :transaction/amount      (rounded 2 (/ amount (+ 1.0 (/ vat 100))))
      :transaction/side        :debit}
     {:transaction/id          id
      :transaction/line 3
      :transaction/date        invoice-date
      :transaction/account     "01234"
      :transaction/description description
      :transaction/cost-center ""
      :transaction/sub-admin   creditor-id
      :transaction/amount      (rounded 2 (- amount (/ amount (+ 1.0 (/ vat 100)))))
      :transaction/side        :debit}]))

(defn pull-transactions []
  (xtdb/q
   (xtdb/db xtdb-node)
   '{:find  [(pull ?trn [*])]
     :where [[?trn :transaction/id _]]}))

(defn pull-transaction-by-id [id]
  (xtdb/q
   (xtdb/db xtdb-node)
   '{:find  [(pull ?trn [{:transaction/account [:account/id :transaction/account :account/name]}
                         :transaction/amount
                         :transaction/side
                         :transaction/description])]
     :in    [?id]
     :where [[?trn :transaction/id ?id]]}
   id))

(comment
  (pull-transaction-by-id #uuid"c8879064-69a1-482c-89e7-589488fdbf7f"))

(defn fetch-transactions []
  (xtdb/q
   (xtdb/db xtdb-node)
   '{:find     [?id ?dt ?ln ?dsc ?anr ?cce ?sad ?amt ?sid]
     :where
     [[?trn :transaction/id ?id]
      [?trn :transaction/date ?dt]
      [?trn :transaction/line ?ln]
      [?trn :transaction/description ?dsc]
      [?trn :transaction/account ?anr]
      [?trn :transaction/cost-center ?cce]
      [?trn :transaction/sub-admin ?sad]
      [?trn :transaction/amount ?amt]
      [?trn :transaction/side ?sid]]
     :order-by [[?id :asc]]}))

(defn fetch-transaction-by-id
  [id]
  (xtdb/q
   (xtdb/db xtdb-node)
   '{:find     [?id ?dt ?ln ?dsc ?anr ?cce ?sad ?amt ?sid]
     :in       [?id]
     :where    [[?trn :transaction/id ?id]
                [?trn :transaction/date ?dt]
                [?trn :transaction/line ?ln]
                [?trn :transaction/description ?dsc]
                [?trn :transaction/account ?anr]
                 ;[?act :account/name ?anm]
                 ;[?act :account/id ?anr]
                [?trn :transaction/cost-center ?cce]
                [?trn :transaction/sub-admin ?sad]
                [?trn :transaction/amount ?amt]
                [?trn :transaction/side ?sid]]
     :order-by [[?id :asc]]}
   id))

(comment
  (count (pull-transactions))
  (count (fetch-transactions))
  (fetch-transaction-by-id #uuid"848d9819-742a-4edd-b8e3-108c0e70f5c8")
  (transact-sales-invoice!
   {:invoice-date     (now)
    :description      "My sales invoice test."
    :debtor-id        "2021-01"
    :amount           14400
    :turnover-account "80100"
    :vat              21})
  (every? true?
          (for [_ (range 2000)]
            (transact! (book-sales-invoice (mg/generate SalesBooking {:seed 10 :size 20})))))

  (balance (book-purchase-invoice (mg/generate PurchaseBooking)))

  (m/explain Transaction
             (let [id (uuid)
                   t-date (now)]
               [{:transaction/id          id
                 :transaction/line 1
                 :transaction/date        t-date
                 :transaction/account     "12345"
                 :transaction/description "bb"
                 :transaction/cost-center ""
                 :transaction/sub-admin   ""
                 :transaction/amount      10.0
                 :transaction/side        :credit}
                {:transaction/id          id
                 :transaction/line 2
                 :transaction/date        t-date
                 :transaction/account     "22334"
                 :transaction/description "bb"
                 :transaction/cost-center ""
                 :transaction/sub-admin   ""
                 :transaction/amount      5.0
                 :transaction/side        :debit}
                {:transaction/id          id
                 :transaction/line 3
                 :transaction/date        t-date
                 :transaction/account     "22442"
                 :transaction/description "bb"
                 :transaction/cost-center ""
                 :transaction/sub-admin   ""
                 :transaction/amount      5.0
                 :transaction/side        :debit}]))
  (mg/generate TransactionLine)
  (line-totals [5.0 0.0] {:transaction/amount 10.0
                          :transaction/side   :credit})
  (balance
   (let [id (uuid)
         t-date (now)]
     [{:transaction/id          id
       :transaction/line 1
       :transaction/date        t-date
       :transaction/account     "12345"
       :transaction/description "bb"
       :transaction/cost-center ""
       :transaction/sub-admin   ""
       :transaction/amount      10.0
       :transaction/side        :credit}
      {:transaction/id          id
       :transaction/line 2
       :transaction/date        t-date
       :transaction/account     "22334"
       :transaction/description "bb"
       :transaction/cost-center ""
       :transaction/sub-admin   ""
       :transaction/amount      2.75
       :transaction/side        :debit}
      {:transaction/id          id
       :transaction/line 3
       :transaction/date        t-date
       :transaction/account     "22442"
       :transaction/description "bb"
       :transaction/cost-center ""
       :transaction/sub-admin   ""
       :transaction/amount      7.25
       :transaction/side        :debit}]))

  (for [x (range 2000)]
    (balance (book-sales-invoice {:invoice-date     (now)
                                  :description      "ha"
                                  :debtor-id        "123"
                                  :amount           (+ 121.564 x)
                                  :turnover-account "80100"
                                  :vat              21}))))



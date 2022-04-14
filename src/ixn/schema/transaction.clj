(ns ixn.schema.transaction
  (:require
    [clojure.tools.logging :as log]
    [ixn.db :refer [transact! xtdb-node]]
    [ixn.schema.account :refer [AccountNumber]]
    [ixn.schema.core :refer [NotEmptyString]]
    [ixn.schema.journal :refer [JournalType journal-params]]
    [ixn.schema.money :refer [->money <-money Money]]
    [ixn.utils :refer [now uuid]]
    [malli.core :as m]
    [malli.generator :as mg]
    [xtdb.api :as xtdb]))


(defonce subadmin-accounts
         {:account-vat-high "27030"
          :vat-high         21
          :account-vat-low  "27040"
          :vat-low          9})

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
   [:transaction/period pos-int?]
   [:transaction/date inst?]
   [:transaction/amount Money]
   [:transaction/sub-admin string?]
   [:transaction/cost-center string?]
   [:transaction/side [:enum {} :debit :credit]]])


(defn- line-totals [s line]
  (let [{:keys [:transaction/amount :transaction/side]} line
        calc-amount (<-money amount)]
    (if (= :debit side)
      [(+ (first s) calc-amount) (last s)]
      [(first s) (+ (last s) calc-amount)])))


(defn- balance [lines]
  (let [[d c] (reduce line-totals [0.0 0.0] lines)]
    (= 0.0 (- d c))))


(def Transaction
  [:and
   [:vector TransactionLine]])


(def SalesBooking
  [:map
   [:invoice-date inst?]
   [:description string?]
   [:debtor-id string?]
   [:amount-high number?]
   [:amount-low number?]
   [:amount-zero number?]
   [:account AccountNumber]
   [:turnover-account AccountNumber]])


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
  (fetch-account "80100")
  (fetch-account "12010"))



(defn- book-sales-invoice
  "prepare a booking for a sales invoice."
  [{:keys [:invoice-date :description :debtor-id :amount-high :amount-low :amount-zero :turnover-account]}]
  (let [id (uuid)
        vat-high (:vat-high subadmin-accounts)
        vat-low (:vat-low subadmin-accounts)
        vat-amt-high (* amount-high (/ vat-high 100.0))
        vat-amt-low (* amount-low (/ vat-low 100))
        invoice-amt (+ amount-high amount-low amount-zero vat-amt-high vat-amt-low)
        turnover-amt (+ amount-high amount-low amount-zero)]
    (->> [{:transaction/id          id
           :transaction/line        1
           :transaction/date        invoice-date
           :transaction/journal     :sales
           :transaction/account     (get-in journal-params [:sales :accounts-receivable])
           :transaction/description description
           :transaction/cost-center ""
           :transaction/sub-admin   debtor-id
           :transaction/amount      (->money invoice-amt)
           :transaction/side        :debit}

          {:transaction/id          id
           :transaction/line        2
           :transaction/date        invoice-date
           :transaction/journal     :sales
           :transaction/account     turnover-account
           :transaction/description description
           :transaction/cost-center ""
           :transaction/sub-admin   debtor-id
           :transaction/amount      (->money turnover-amt)
           :transaction/side        :credit}

          {:transaction/id          id
           :transaction/line        3
           :transaction/date        invoice-date
           :transaction/journal     :sales
           :transaction/account     (:account-vat-high subadmin-accounts)
           :transaction/description description
           :transaction/cost-center ""
           :transaction/sub-admin   debtor-id
           :transaction/amount      (->money vat-amt-high)
           :transaction/side        :credit}

          {:transaction/id          id
           :transaction/line        4
           :transaction/date        invoice-date
           :transaction/journal     :sales
           :transaction/account     (:account-vat-low subadmin-accounts)
           :transaction/description description
           :transaction/cost-center ""
           :transaction/sub-admin   debtor-id
           :transaction/amount      (->money vat-amt-low)
           :transaction/side        :credit}]
         (filter (fn [x] (not= 0 (get-in x [:transaction/amount :money/value])))))))


(defn transact-sales-invoice!
  [params]
  "Check and store a sales transaction."
  (let [booking (book-sales-invoice params)]
    (if (balance booking)
      (transact! booking)
      (log/error (str "Sales invoice balance for booking " params " is not 0.00")))))

(comment
  (for [x (range 10000)]
    (do
      (prn "transaction: " x)
      (transact-sales-invoice! {:invoice-date  (now)
                                :description   "ha"
                                :debtor-id     "123"
                                :amount-high   100
                                :amount-low    10
                                :amount-zero   0
                                :costs-account "80100"})))

  ())


(def PurchaseBooking
  [:map
   [:invoice-date inst?]
   [:description string?]
   [:creditor-id string?]
   [:amount-high number?]
   [:amount-low number?]
   [:amount-zero number?]
   [:costs-account [:re
                    {:error-message {:en "Only 5 digit accounts accept bookings."
                                     :nl "Alleen 5 cijferige rekeningen accepteren een boeking."}}
                    #"^[0-9]{5}$"]]])

(defn- book-purchase-invoice
  "prepare a booking for a sales invoice."

  [{:keys [:invoice-date :description :creditor-id :amount-high :amount-low :amount-zero :costs-account]}]
  (let [id (uuid)
        vat-high (:vat-high subadmin-accounts)
        vat-low (:vat-low subadmin-accounts)
        vat-amt-high (* amount-high (/ vat-high 100.0))
        vat-amt-low (* amount-low (/ vat-low 100))
        invoice-amt (+ amount-high amount-low amount-zero vat-amt-high vat-amt-low)
        costs-amt (+ amount-high amount-low amount-zero)]
    (->> [{:transaction/id          id
           :transaction/line        1
           :transaction/date        invoice-date
           :transaction/journal     :purchase
           :transaction/account     (get-in journal-params [:purchase :accounts-payable])
           :transaction/description description
           :transaction/cost-center ""
           :transaction/sub-admin   creditor-id
           :transaction/amount      (->money invoice-amt)
           :transaction/side        :credit}
          {:transaction/id          id
           :transaction/line        2
           :transaction/date        invoice-date
           :transaction/journal     :purchase
           :transaction/account     costs-account
           :transaction/description description
           :transaction/cost-center ""
           :transaction/sub-admin   creditor-id
           :transaction/amount      (->money costs-amt)
           :transaction/side        :debit}
          {:transaction/id          id
           :transaction/line        3
           :transaction/date        invoice-date
           :transaction/journal     :purchase
           :transaction/account     (:account-vat-high subadmin-accounts)
           :transaction/description description
           :transaction/cost-center ""
           :transaction/sub-admin   creditor-id
           :transaction/amount      (->money vat-amt-high)
           :transaction/side        :debit}
          {:transaction/id          id
           :transaction/line        4
           :transaction/date        invoice-date
           :transaction/journal     :purchase
           :transaction/account     (:account-vat-low subadmin-accounts)
           :transaction/description description
           :transaction/cost-center ""
           :transaction/sub-admin   creditor-id
           :transaction/amount      (->money vat-amt-low)
           :transaction/side        :debit}]
         (filter (fn [x] (not= 0 (get-in x [:transaction/amount :money/value])))))))

(comment
  (->
    (book-purchase-invoice {:invoice-date  (now)
                            :description   "ha"
                            :creditor-id   "123"
                            :amount-high   100
                            :amount-low    10
                            :amount-zero   0
                            :costs-account "80100"})
    balance)

  ())

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
                 [?act :account/name ?anm]
                 [?act :account/id ?anr]
                 [?trn :transaction/cost-center ?cce]
                 [?trn :transaction/sub-admin ?sad]
                 [?trn :transaction/amount ?amt]
                 [?trn :transaction/side ?sid]]
      :order-by [[?id :asc]]}
    id))

(comment
  (count (pull-transactions))
  (count (fetch-transactions))
  (fetch-transaction-by-id #uuid"73786575-9e90-4ce0-a18b-c64326c64a5a")
  (transact-sales-invoice!
    {:invoice-date     (now)
     :description      "My sales invoice test."
     :debtor-id        "2021-01"
     :amount-high      144.00
     :amount-low       0
     :amount-zero      0
     :turnover-account "80100"})
  (every? true?
          (for [_ (range 2000)]
            (transact! (book-sales-invoice (mg/generate SalesBooking {:seed 10 :size 20})))))

  (balance (book-purchase-invoice (mg/generate PurchaseBooking)))

  (m/explain Transaction
             (let [id (uuid)
                   t-date (now)]
               [{:transaction/id          id
                 :transaction/line        1
                 :transaction/date        t-date
                 :transaction/account     "12345"
                 :transaction/description "bb"
                 :transaction/cost-center ""
                 :transaction/sub-admin   ""
                 :transaction/amount      10.0
                 :transaction/side        :credit}
                {:transaction/id          id
                 :transaction/line        2
                 :transaction/date        t-date
                 :transaction/account     "22334"
                 :transaction/description "bb"
                 :transaction/cost-center ""
                 :transaction/sub-admin   ""
                 :transaction/amount      5.0
                 :transaction/side        :debit}
                {:transaction/id          id
                 :transaction/line        3
                 :transaction/date        t-date
                 :transaction/account     "22442"
                 :transaction/description "bb"
                 :transaction/cost-center ""
                 :transaction/sub-admin   ""
                 :transaction/amount      5.0
                 :transaction/side        :debit}]))
  (mg/generate TransactionLine)
  (line-totals [5.0 0.0] {:transaction/amount (->money 10.0)
                          :transaction/side   :credit})
  (balance
    (let [id (uuid)
          t-date (now)]
      [{:transaction/id          id
        :transaction/line        1
        :transaction/date        t-date
        :transaction/account     "12345"
        :transaction/description "bb"
        :transaction/cost-center ""
        :transaction/sub-admin   ""
        :transaction/amount      10.0
        :transaction/side        :credit}
       {:transaction/id          id
        :transaction/line        2
        :transaction/date        t-date
        :transaction/account     "22334"
        :transaction/description "bb"
        :transaction/cost-center ""
        :transaction/sub-admin   ""
        :transaction/amount      2.75
        :transaction/side        :debit}
       {:transaction/id          id
        :transaction/line        3
        :transaction/date        t-date
        :transaction/account     "22442"
        :transaction/description "bb"
        :transaction/cost-center ""
        :transaction/sub-admin   ""
        :transaction/amount      7.25
        :transaction/side        :debit}]))

  (for [x (range 20)]
    (balance (book-sales-invoice {:invoice-date     (now)
                                  :description      "ha"
                                  :debtor-id        "123"
                                  :amount           (+ 121.564 x)
                                  :turnover-account "80100"
                                  :vat              21}))))



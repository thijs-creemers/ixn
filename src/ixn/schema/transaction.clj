(ns ixn.schema.transaction
  (:require
   [clojure.tools.logging :as log]
   [crux.api :as crux]
   [malli.core :as m]
   [malli.generator :as mg]
   [ixn.db :refer [crux-node transact!]]
   [ixn.utils :refer [uuid now]]
   [ixn.money :refer [rounded Money]]
   [ixn.schema.account :refer [AccountNumber]]
   [ixn.schema.core :refer [NotEmptyString]]))


(def TransactionLine
  [:map
   {:closed true
    :title  {:en "Transaction line" :nl "Transactie regel"}}
   [:transaction/id uuid?]
   [:transaction/line-number pos-int?]
   [:transaction/account-number AccountNumber]
   [:transaction/description NotEmptyString]
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
   [:turnover-account [:re
                       {:error-message {:en "Only 5 digit accounts accept bookings."
                                        :nl "Alleen 5 cijferige rekeningen accepteren een boeking."}}
                       #"^[0-9]{5}$"]]
   [:vat [:and [:and
                decimal?
                [:>= 3.0]
                [:<= 25.0]]]]])

(defn- book-sales-invoice
  [{:keys [:invoice-date :description :debtor-id :amount :turnover-account :vat]}]
  (let [id (uuid)
        invoice-amt (rounded amount)
        turnover-amt (rounded (/ invoice-amt (+ 1.0 (/ vat 100.0))))
        vat-amt (- invoice-amt turnover-amt)]

    [{:transaction/id             id
      :transaction/line-number    1
      :transaction/date           invoice-date
      :transaction/account-number "12010"
      :transaction/description    description
      :transaction/cost-center    ""
      :transaction/sub-admin      debtor-id
      :transaction/amount         invoice-amt
      :transaction/side           :debit}

     {:transaction/id             id
      :transaction/line-number    2
      :transaction/date           invoice-date
      :transaction/account-number turnover-account
      :transaction/description    description
      :transaction/cost-center    ""
      :transaction/sub-admin      debtor-id
      :transaction/amount         turnover-amt
      :transaction/side           :credit}

     {:transaction/id             id
      :transaction/line-number    3
      :transaction/date           invoice-date
      :transaction/account-number "01234"
      :transaction/description    description
      :transaction/cost-center    ""
      :transaction/sub-admin      debtor-id
      :transaction/amount         vat-amt
      :transaction/side           :credit}]))

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
    [{:transaction/id             id
      :transaction/line-number    1
      :transaction/date           invoice-date
      :transaction/account-number "15000"
      :transaction/description    description
      :transaction/cost-center    ""
      :transaction/sub-admin      creditor-id
      :transaction/amount         (rounded 2 amount)
      :transaction/side           :credit}
     {:transaction/id             id
      :transaction/line-number    2
      :transaction/date           invoice-date
      :transaction/account-number turnover-account
      :transaction/description    description
      :transaction/cost-center    ""
      :transaction/sub-admin      creditor-id
      :transaction/amount         (rounded 2 (/ amount (+ 1.0 (/ vat 100))))
      :transaction/side           :debit}
     {:transaction/id             id
      :transaction/line-number    3
      :transaction/date           invoice-date
      :transaction/account-number "01234"
      :transaction/description    description
      :transaction/cost-center    ""
      :transaction/sub-admin      creditor-id
      :transaction/amount         (rounded 2 (- amount (/ amount (+ 1.0 (/ vat 100)))))
      :transaction/side           :debit}]))

(defn pull-transactions []
  (crux/q
    (crux/db crux-node)
    '{:find [(pull ?trn [*])]
      :where [[?trn :transaction/id _]]}))

(defn fetch-transactions []
  (crux/q
    (crux/db crux-node)
    '{:find [?trn ?id ?dt ?ln ?dsc ?anr ?cce ?sad ?amt ?sid]
      :where [[?trn :transaction/id ?id]
              [?trn :transaction/date ?dt]
              [?trn :transaction/line-number ?ln]
              [?trn :transaction/description ?dsc]
              [?trn :transaction/account-number ?anr]
              [?trn :transaction/cost-center ?cce]
              [?trn :transaction/sub-admin   ?sad]
              [?trn :transaction/amount ?amt]
              [?trn :transaction/side   ?sid]]
      :order-by [[?id :asc]]}))

(comment
  (pull-transactions)
  (fetch-transactions)
  (transact-sales-invoice!
    {:invoice-date (now)
     :description "My sales invoice test."
     :debtor-id        "2021-01"
     :amount           14400
     :turnover-account "80200"
     :vat              21})
  (every? true?
    (for [_ (range 20)]
      (book-sales-invoice (mg/generate SalesBooking {:seed 10 :size 20}))))

  (balance (book-purchase-invoice (mg/generate PurchaseBooking)))

  (m/explain Transaction
             (let [id (uuid)
                   t-date (now)]
               [{:transaction/id             id
                 :transaction/line-number    1
                 :transaction/date           t-date
                 :transaction/account-number "12345"
                 :transaction/description    "bb"
                 :transaction/cost-center    ""
                 :transaction/sub-admin      ""
                 :transaction/amount         10.0
                 :transaction/side           :credit}
                {:transaction/id             id
                 :transaction/line-number    2
                 :transaction/date           t-date
                 :transaction/account-number "22334"
                 :transaction/description    "bb"
                 :transaction/cost-center    ""
                 :transaction/sub-admin      ""
                 :transaction/amount         5.0
                 :transaction/side           :debit}
                {:transaction/id             id
                 :transaction/line-number    3
                 :transaction/date           t-date
                 :transaction/account-number "22442"
                 :transaction/description    "bb"
                 :transaction/cost-center    ""
                 :transaction/sub-admin      ""
                 :transaction/amount         5.0
                 :transaction/side           :debit}]))
  (mg/generate TransactionLine)
  (line-totals [5.0 0.0] {:transaction/amount 10.0
                          :transaction/side   :credit})
  (balance
    (let [id (uuid)
          t-date (now)]
      [{:transaction/id             id
        :transaction/line-number    1
        :transaction/date           t-date
        :transaction/account-number "12345"
        :transaction/description    "bb"
        :transaction/cost-center    ""
        :transaction/sub-admin      ""
        :transaction/amount         10.0
        :transaction/side           :credit}
       {:transaction/id             id
        :transaction/line-number    2
        :transaction/date           t-date
        :transaction/account-number "22334"
        :transaction/description    "bb"
        :transaction/cost-center    ""
        :transaction/sub-admin      ""
        :transaction/amount         2.75
        :transaction/side           :debit}
       {:transaction/id             id
        :transaction/line-number    3
        :transaction/date           t-date
        :transaction/account-number "22442"
        :transaction/description    "bb"
        :transaction/cost-center    ""
        :transaction/sub-admin      ""
        :transaction/amount         7.25
        :transaction/side           :debit}]))

  (for [x (range 2000)]
    (balance (book-sales-invoice {:invoice-date     (now)
                                  :description      "ha"
                                  :debtor-id        "123"
                                  :amount           (+ 121.564 x)
                                  :turnover-account "80000"
                                  :vat              21}))))



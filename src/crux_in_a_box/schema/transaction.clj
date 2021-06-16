(ns crux-in-a-box.schema.transaction
  (:require
    [crux-in-a-box.money :refer [rounded Money]]
    [crux-in-a-box.schema.account :refer [AccountNumber]]
    [crux-in-a-box.schema.core :refer [NotEmptyString]]
    [malli.core :as m]
    [malli.generator :as mg]))

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

(defn uuid []
  (java.util.UUID/randomUUID))
(defn date []
  (java.util.Date.))

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

(defn book-sales-invoice
  [{:keys [:invoice-date :description :debtor-id :amount :turnover-account :vat]}]
  (let [id (uuid)
        invoice-amt (rounded amount)
        turnover-amt (rounded (/ invoice-amt (+ 1.0 (/ vat 100.0))))
        vat-amt (- invoice-amt turnover-amt)]

    [{:transaction/id             id
      :transaction/line-number    1
      :transaction/date           invoice-date
      :transaction/account-number "13000"
      :transaction/description    description
      :transaction/cost-center    ""
      :transaction/sub-admin      debtor-id
      :transaction/amount         invoice-amt
      :transaction/side           :debit}

     {:transaction/id             id
      :transaction/line-number    2
      :transaction/date           date
      :transaction/account-number turnover-account
      :transaction/description    description
      :transaction/cost-center    ""
      :transaction/sub-admin      debtor-id
      :transaction/amount         turnover-amt
      :transaction/side           :credit}

     {:transaction/id             id
      :transaction/line-number    3
      :transaction/date           date
      :transaction/account-number "01234"
      :transaction/description    description
      :transaction/cost-center    ""
      :transaction/sub-admin      debtor-id
      :transaction/amount         vat-amt
      :transaction/side           :credit}]))

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
      :transaction/date           date
      :transaction/account-number turnover-account
      :transaction/description    description
      :transaction/cost-center    ""
      :transaction/sub-admin      creditor-id
      :transaction/amount         (rounded 2 (/ amount (+ 1.0 (/ vat 100))))
      :transaction/side           :debit}
     {:transaction/id             id
      :transaction/line-number    3
      :transaction/date           date
      :transaction/account-number "01234"
      :transaction/description    description
      :transaction/cost-center    ""
      :transaction/sub-admin      creditor-id
      :transaction/amount         (rounded 2 (- amount (/ amount (+ 1.0 (/ vat 100)))))
      :transaction/side           :debit}]))


(comment
  (every? true?
    (for [x (range 20)]
      (book-sales-invoice (mg/generate SalesBooking {:seed 10 :size 20}))))

  (balance (book-purchase-invoice (mg/generate PurchaseBooking)))

  (m/explain Transaction
             (let [id (uuid)
                   t-date (date)]
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
          t-date (date)]
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
    (balance (book-sales-invoice {:invoice-date     #inst"1970-01-01T00:00:00.005-00:00"
                                  :description      "ha"
                                  :debtor-id        "123"
                                  :amount           (+ 121.564 x)
                                  :turnover-account "80000"
                                  :vat              21}))))



(ns ixn.financial.receivable.transaction
  (:require
   [clojure.tools.logging :as log]
   [ixn.utils :refer [now uuid]]
   [ixn.settings :refer [subadmin-accounts]]
   [ixn.db :refer [transact!]]
   [ixn.schema.money :refer [->money]]
   [ixn.schema.journal :refer [journal-params]]
   [ixn.financial.utils :refer [balance]]))

(defn book-sales-invoice
  "Prepare sales invoice booking."
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
  "Check and store a sales transaction."
  [params]
  (let [booking (book-sales-invoice params)]
    (if (balance booking)
      (transact! booking)
      (log/error (str "Sales invoice balance for booking " params " is not 0.00")))))

(comment
  "Some checks to perform on REPL"
  (for [x (range 10000)]
    (do
      (prn "transaction: " x)
      (transact-sales-invoice!
       {:invoice-date  (now)
        :description   "ha"
        :debtor-id     "123"
        :amount-high   100
        :amount-low    10
        :amount-zero   0
        :turnover-account "80100"})))

  (transact-sales-invoice!
   {:invoice-date     (now)
    :description      "My sales invoice test."
    :debtor-id        "2021-01"
    :amount-high      144.00
    :amount-low       0
    :amount-zero      0
    :turnover-account "80100"}
   (every? true?
           (for [_ (range 2000)]
             (transact! (book-sales-invoice (mg/generate SalesBooking {:seed 10 :size 20})))))

   ()))

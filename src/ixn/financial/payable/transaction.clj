(ns ixn.financial.payable.transaction
  (:require
    [clojure.tools.logging :as log]
    [ixn.utils :refer [now uuid]]
    [ixn.settings :refer [subadmin-accounts]]
    [ixn.db :refer [transact!]]
    [ixn.schema.money :refer [->money]]
    [ixn.schema.journal :refer [journal-params]]
    [ixn.financial.utils :refer [balance]]))


(defn- book-purchase-invoice
  "prepare a booking for a sales invoice."

  [{:keys [:invoice-date :description :creditor-id :amount-high :amount-low :amount-zero :costs-account]}]
  (let [id (uuid)
        vat-high (:vat-high subadmin-accounts)
        vat-low (:vat-low subadmin-accounts)
        vat-amt-high (bigdec (* amount-high (/ vat-high 100.0)))
        vat-amt-low (bigdec (* amount-low (/ vat-low 100)))
        invoice-amt (bigdec (+ amount-high amount-low amount-zero vat-amt-high vat-amt-low))
        costs-amt (bigdec (+ amount-high amount-low amount-zero))]
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


(defn transact-purchase-invoice!
  "Check and store a purchase transaction."
  [params]
  (let [booking (book-purchase-invoice params)]
    (if (balance booking)
      (transact! booking)
      (log/error (str "Purchase invoice balance for booking " params " is not 0.00")))))


(comment
  ;; Transact 1000 purchase order invoices.
  (time
    (for [x (range 1000)]
      (do
        (prn "transaction: " x)
        (->
          {:invoice-date  (now)
           :description   "ha"
           :creditor-id   "123"
           :amount-high   100
           :amount-low    10
           :amount-zero   0
           :costs-account "80100"}
          transact-purchase-invoice!))))

  ())


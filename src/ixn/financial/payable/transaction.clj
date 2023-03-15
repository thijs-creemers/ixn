(ns ixn.financial.payable.transaction
  (:require
    [clojure.tools.logging :as log]
    [ixn.db :refer [transact!]]
    [ixn.financial.utils :refer [balance]]
    [ixn.schema.journal :refer [journal-params]]
    [ixn.schema.money :refer [->money]]
    [ixn.schema.transaction :refer [PurchaseBooking TransactionLine]]
    [ixn.settings :refer [current-book-period subadmin-accounts]]
    [ixn.state :refer [xtdb-node]]
    [ixn.utils :refer [now uuid]]
    [malli.core :as m]
    [malli.error :as me]
    [xtdb.api :as xtdb]))


(defn pull-all-purchase-invoices
  [{:keys [sort-on order limit offset]}]

  (let [transaction-account (get-in journal-params [:purchase :accounts-payable])
        my-limit (if (and limit (> limit 0)) limit 10)
        my-offset (if offset offset 0)
        data (->> (xtdb/q
                    (xtdb/db (xtdb-node))
                    '{:find  [(pull ?trn [*])]
                      :in    [?transaction-account]
                      :where [[?trn :transaction/id _]
                              [?trn :transaction/journal :purchase]
                              [?trn :transaction/account ?transaction-account]]}
                    transaction-account)
                  (map first)
                  (sort #(compare (sort-on %1) (sort-on %2)))
                  (drop my-offset)
                  (take my-limit))]
    (if (= order :asc) data (reverse data))))


(defn pull-purchase-invoices-by-subadmin [subadmin-id]
  (let [transaction-account (get-in journal-params [:purchase :accounts-payable])]
    (xtdb/q
      (xtdb/db (xtdb-node))
      '{:find  [(pull ?trn [*])]
        :in    [?subadmin-id ?transaction-account]
        :where [[?trn :transaction/id _]
                [?trn :transaction/journal :purchase]
                [?trn :transaction/sub-admin ?subadmin-id]
                [?trn :transaction/account ?transaction-account]]}
      subadmin-id transaction-account)))


(defn book-purchase-invoice
  "prepare a booking for a sales invoice."

  [{:keys [:invoice-date :description :creditor-id :amount-high :amount-low :amount-zero :costs-account :invoice] :as booking}]
  (if (not (m/validate PurchaseBooking booking))
    ((log/error "Not a valid booking")
     (prn (me/humanize (m/explain PurchaseBooking booking))))
    (let [id           (uuid)
          vat-high     (:vat-high subadmin-accounts)
          vat-low      (:vat-low subadmin-accounts)
          vat-amt-high (bigdec (* amount-high (/ vat-high 100.0)))
          vat-amt-low  (bigdec (* amount-low (/ vat-low 100)))
          invoice-amt  (bigdec (+ amount-high amount-low amount-zero vat-amt-high vat-amt-low))
          costs-amt    (bigdec (+ amount-high amount-low amount-zero))
          acc-payable  {:transaction/id          id
                        :transaction/line        1
                        :transaction/date        invoice-date
                        :transaction/year        (:year current-book-period)
                        :transaction/period      (:period current-book-period)
                        :transaction/journal     :purchase
                        :transaction/account     (get-in journal-params [:purchase :accounts-payable])
                        :transaction/description description
                        :transaction/cost-center ""
                        :transaction/sub-admin   creditor-id
                        :transaction/amount      (->money invoice-amt)
                        :transaction/side        :credit
                        :transaction/invoice     invoice}

          costs        {:transaction/id          id
                        :transaction/line        2
                        :transaction/date        invoice-date
                        :transaction/year        (:year current-book-period)
                        :transaction/period      (:period current-book-period)
                        :transaction/journal     :purchase
                        :transaction/account     costs-account
                        :transaction/description description
                        :transaction/cost-center ""
                        :transaction/sub-admin   creditor-id
                        :transaction/amount      (->money costs-amt)
                        :transaction/invoice     invoice
                        :transaction/side        :debit}


          vat-high     {:transaction/id          id
                        :transaction/line        3
                        :transaction/date        invoice-date
                        :transaction/year        (:year current-book-period)
                        :transaction/period      (:period current-book-period)
                        :transaction/journal     :purchase
                        :transaction/account     (:account-vat-high subadmin-accounts)
                        :transaction/description description
                        :transaction/cost-center ""
                        :transaction/sub-admin   creditor-id
                        :transaction/amount      (->money vat-amt-high)
                        :transaction/invoice     invoice
                        :transaction/side        :debit}

          vat-low      {:transaction/id          id
                        :transaction/line        4
                        :transaction/date        invoice-date
                        :transaction/year        (:year current-book-period)
                        :transaction/period      (:period current-book-period)
                        :transaction/journal     :purchase
                        :transaction/description description
                        :transaction/account     (:account-vat-low subadmin-accounts)
                        :transaction/cost-center ""
                        :transaction/sub-admin   creditor-id
                        :transaction/amount      (->money vat-amt-low)
                        :transaction/invoice     invoice
                        :transaction/side        :debit}
          valid?       (every? true?
                               (map (fn [x] (m/validate TransactionLine x)) [acc-payable costs vat-high vat-low]))]
      (if valid?
        (->> [acc-payable costs vat-high vat-low]
             (filter (fn [x] (not= 0 (get-in x [:transaction/amount :money/value])))))
        (log/error "One of the transaction lines is invalid.")))))

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
    (for [x (range 2)]
      (do
        (prn "transaction: " x)
        (-> {:invoice-date  (now)
             :description   "ha"
             :creditor-id   "222"
             :amount-high   100
             :amount-low    10
             :amount-zero   0
             :invoice       (str "YYFF-" (rand-int 20000))
             :costs-account "80100"}
            transact-purchase-invoice!))))

  (m/validate PurchaseBooking {:invoice-date  (now)
                               :description   "ha"
                               :creditor-id   "222"
                               :amount-high   100
                               :amount-low    10
                               :amount-zero   0
                               :invoice       (str "YYFF-" (rand-int 20000))
                               :costs-account "80100"})

  (m/validate TransactionLine {:transaction/id          (uuid)
                               :transaction/line        1
                               :transaction/date        (now)
                               :transaction/year        2023
                               :transaction/period      3
                               :transaction/journal     :purchase
                               :transaction/account     (get-in journal-params [:purchase :accounts-payable])
                               :transaction/description "description of this line"
                               :transaction/cost-center ""
                               :transaction/sub-admin   "222"
                               :transaction/amount      (->money 100)
                               :transaction/invoice     ""
                               :transaction/side :credit})

  (pull-all-purchase-invoices {:sort-on :transaction/id :order :asc :limit 0 :offset 20})
  (pull-purchase-invoices-by-subadmin "222")
  ())

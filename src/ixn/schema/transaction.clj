(ns ixn.schema.transaction
  (:require
   [ixn.db :refer [xtdb-node]]
   [ixn.schema.account :refer [AccountNumber]]
   [ixn.schema.core :refer [NotEmptyString]]
   [ixn.schema.journal :refer [JournalType]]
   [ixn.schema.money :refer [->money Money]]
   [ixn.financial.utils :refer [balance line-totals]]
   [ixn.financial.receivable.transaction :refer [book-sales-invoice]]
   [ixn.utils :refer [now uuid]]
   [malli.core :as m]
   [malli.generator :as mg]
   [xtdb.api :as xtdb]))

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

(ns ixn.schema.account
  (:require [clojure.tools.logging :as log]
            [clojure.tools.reader.edn :as edn]
            [xtdb.api :as xtdb]
            [ixn.state :refer [xtdb-node]]
            [ixn.db :refer [transact!]]
            [ixn.schema.core :refer [NotEmptyString]]
            [malli.core :as m]))

;;
;; What does debit or credit mean!
;; +----------------------+----------+----------+------+
;; |Kind of account       |  Debit   | Credit   |      |
;; +----------------------+----------+----------+------+
;; |Asset                 | Increase | Decrease | :ast |
;; |Liability             | Decrease | Increase | :lia |
;; |Income/Revenue        | Decrease | Increase | :prf |
;; |Expense/Cost/Dividend | Increase | Decrease | :cst |
;; |equity/capital        | decrease | increase | :ety |
;; +----------------------+----------+----------+------+

;; Schema definitions
(def AccountNumber [:re
                    {:error-message {:en "An account number must be a 1 to 5 a digit number."
                                     :nl "Een rekeningnummer kan 1 to 5 cijfers lang zijn."}}
                    #"^[0-9]{1,5}$"])

(def AccountType [:enum {:title "Account types"} :ast :ety :lia :cst :prf])

(def SummaryLevel [:and int? [:>= 0] [:<= 4]])

(def Account
  [:map
   {:closed? true :title   {:en "account" :nl "rekening"}}
   [:account/id {:title {:en "account number" :nl "rekeningnummer"}} AccountNumber]
   [:account/name {:title {:en "name" :nl "naam"}} NotEmptyString]
   [:account/type {:title {:en "account type" :nl "rekening type"}} AccountType]
   [:account/summary-level {:title {:en "summary level" :nl "verdichtingsniveau"}} SummaryLevel]]) ; summary level is function of id


;; Functions
(defn calc-summary-level
  "The summary level indicates the level of summary,
   only accounts with a summary-level of 0 can be used directly in transactions.
   Accounts with a summary level of 1 to 4 are for reporting summarized views."
  [account-number]
  (if (<= (count account-number) 5)
    (nth (reverse (range 6)) (count account-number))
    -1))


(defn create-account
  "Create an account type, the summary level is calculated from the account-number"
  [{:account/keys [id name type]}]
  (let [new-account {:account/id            id
                     :account/name          name
                     :account/type          type
                     :account/summary-level (calc-summary-level id)}
        valid? (m/validate Account new-account)]
    {:status valid?
     :value  (if valid?
               new-account
               (let [reason (m/explain Account new-account)]
                 (log/error (str "account/create-account: "))

                 (:errors reason)))}))

(comment
  (create-account {:account/id "12000" :account/name "een rekening" :account/type :ast}))


(defn fetch-accounts []
  (xtdb/q
   (xtdb/db (xtdb-node))
   '{:find     [?act ?id ?nm ?tp ?lvl]
     :where    [[?act :account/id ?id]
                [?act :account/name ?nm]
                [?act :account/type ?tp]
                [?act :account/summary-level ?lvl]
                [?act :account/summary-level 0]]
     :order-by [[?id :asc]]}))

(comment (fetch-accounts))


(defn fetch-accounts-by-summary-level [lvl]
  (xtdb/q
   (xtdb/db (xtdb-node))
   '{:find     [?act ?id ?nm ?tp ?lvl]
     :where    [[?act :account/id ?id]
                [?act :account/name ?nm]
                [?act :account/type ?tp]
                [?act :account/summary-level ?lvl]]
     :in       [?lvl]
     :order-by [[?id :asc]]}
   lvl))

(comment (fetch-accounts-by-summary-level 1))


(defn fetch-account-by-id
  "Fetch an account by id"
  [id]
  (xtdb/q
   (xtdb/db (xtdb-node))
   '{:find     [?act ?id ?nm ?tp ?lvl]
     :where    [[?act :account/id ?id]
                [?act :account/name ?nm]
                [?act :account/type ?tp]
                [?act :account/summary-level ?lvl]]
                 ;[?act :account/summary-level 0]]
     :in       [?id]
     :order-by [[?id :asc]]}
   id))

(comment
  (fetch-account-by-id "80100"))


(defn pull-account-by-id
  [id]
  (xtdb/pull (xtdb/db (xtdb-node)) [:account/id :account/name :account/type :account/summary-level] id)
  (ffirst
   (xtdb/q
    (xtdb/db (xtdb-node))
    '{:find [(pull ?act [*])]
      :in [?id]
      :where [[?act :account/id ?id]]}
    id)))

(comment
  (pull-account-by-id "12010"))


(defn pull-all-accounts
  "return all accounts with summary level 0"
  [{:keys [sort-on order limit offset]}]

  (let [my-limit (if (and limit (> limit 0)) limit 10)
        my-offset (if offset offset 0)
        data (->> (xtdb/q
                    (xtdb/db (xtdb-node))
                    '{:find [(pull ?account [*])]
                      :where [[?account :account/summary-level 0]]})
                  (map first)
                  (sort #(compare (sort-on %1) (sort-on %2)))
                  (drop my-offset)
                  (take my-limit))]
    (if (= order :asc) data (reverse data))))

(comment
  (pull-all-accounts {:sort-on :account/id :order :asc}))


(defn fetch-accounts-by-type
  "Fetch an account by account-type"
  [tp]
  (xtdb/q
   (xtdb/db (xtdb-node))
   '{:find     [?act ?id ?nm ?tp ?lvl]
     :where    [[?act :account/id ?id]
                [?act :account/name ?nm]
                [?act :account/type ?tp]
                [?act :account/summary-level ?lvl]
                [?act :account/summary-level 0]]
     :in       [?tp]
     :order-by [[?id :asc]]}
   tp))


(defn import-accounts-fixture
  []
  (let [fixture (edn/read-string (slurp "resources/fixtures/accounts.edn"))]
    (transact! (->> fixture
                    (map #(create-account %))
                    (filter #(:status %))
                    (mapv #(:value %))
                    vec))))

(comment
  "Some repl testing code"
  (count (fetch-accounts))
  (time (fetch-account-by-id "80200"))
  (time (fetch-accounts-by-type :cst))
  (import-accounts-fixture)
  (fetch-accounts-by-summary-level 2)
  (pull-account-by-id "80200")
  (m/validate AccountNumber "40010")
  (m/explain AccountNumber "400100")
  (let [account {:account/id            "800100"
                 :account/name          "Turnover High VAT"
                 :account/type          :prf
                 :account/summary-level 0}]
    ;(m/validate Account account)
    (m/explain Account account)))

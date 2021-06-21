(ns ixn.schema.account
  (:require
   [clojure.tools.logging :as log]
   [clojure.tools.reader.edn :as edn]
   [ixn.schema.core :refer [NotEmptyString]]
  ;;  [ixn.utils :refer [uuid]]
   [crux.api :as crux]
   [ixn.db :refer [crux-node transact!]]
   [malli.core :as m]))

;;
;; What does debit or credit mean!
;; +----------------------+----------+----------+
;; |Kind of account       |  Debit   | Credit   |
;; +----------------------+----------+----------+
;; |Asset                 | Increase | Decrease |
;; |Liability             | Decrease | Increase |
;; |Income/Revenue        | Decrease | Increase |
;; |Expense/Cost/Dividend | Increase | Decrease |
;; |Equity/Capital        | Decrease | Increase |
;; +----------------------+----------+----------+

;; Schema definitions
(def AccountNumber [:re
                    {:error-message {:en "An account number must be a 1 to 5 a digit number."
                                     :nl "Een rekeningnummer kan 1 to 5 cijfers lang zijn."}}
                    #"^[0-9]{1,5}$"])

(def AccountType [:enum {:title "Account types"} :ast :lia :cst :prf])
(def SummaryLevel [:and int? [:>= 0] [:<= 4]])
(def Account
  [:map
   {:closed? true}
   [:account/id AccountNumber]
   [:account/name NotEmptyString]
   [:account/type AccountType]
   [:account/summary-level SummaryLevel]]) ; summary level is function of id

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
     :value (if valid?
              new-account
              (let [reason (m/explain Account new-account)]
                (log/error (str "account/create-account: "))

                (:errors reason)))}))

(defn fetch-accounts []
  (crux/q
    (crux/db crux-node)
    '{:find [?act ?id ?nm ?tp ?lvl]
      :where [[?act :account/id ?id]
              [?act :account/name ?nm]
              [?act :account/type ?tp]
              [?act :account/summary-level ?lvl]
              [?act :account/summary-level 0]]
      :order-by [[?id :asc]]}))

(defn fetch-accounts-by-summary-level [lvl]
  (crux/q
    (crux/db crux-node)
    '{:find [?act ?id ?nm ?tp ?lvl]
      :where [[?act :account/id ?id]
              [?act :account/name ?nm]
              [?act :account/type ?tp]
              [?act :account/summary-level ?lvl]]
      :in [?lvl]
      :order-by [[?id :asc]]}
    lvl))

(defn fetch-account-by-id
  "Fetch an account by id"
  [id]
  (crux/q
    (crux/db crux-node)
    '{:find [?act ?id ?nm ?tp ?lvl]
      :where [[?act :account/id ?id]
              [?act :account/name ?nm]
              [?act :account/type ?tp]
              [?act :account/summary-level ?lvl]
              [?act :account/summary-level 0]]
      :in [?id]
      :order-by [[?id :asc]]}
    id))

(defn fetch-accounts-by-type
  "Fetch an account by account-type"
  [tp]
  (crux/q
    (crux/db crux-node)
    '{:find [?act ?id ?nm ?tp ?lvl]
      :where [[?act :account/id ?id]
              [?act :account/name ?nm]
              [?act :account/type ?tp]
              [?act :account/summary-level ?lvl]
              [?act :account/summary-level 0]]
      :in [?tp]
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

  (m/validate AccountNumber "40010")
  (m/explain AccountNumber "400100")
  (let [account {:account/id            "800100"
                 :account/name          "Turnover High VAT"
                 :account/type          :prf
                 :account/summary-level 0}]
    ;(m/validate Account account)
    (m/explain Account account))
  ,)

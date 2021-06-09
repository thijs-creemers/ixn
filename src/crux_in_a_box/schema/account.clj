(ns crux-in-a-box.schema.account
  (:require [crux-in-a-box.schema.core :refer [NotEmptyString]]
            [clojure.tools.logging :as log]
            ;[crux.api :as crux]
            ;[crux-in-a-box.db :as db]
            [malli.generator :as mg]
            [malli.core :as m]
            [malli.error :as me]
            [clojure.tools.reader.edn :as edn]))

;; Schema definitions
(def AccountNumber [:re
                    {:error-message {:en "An account number must be a 1 to 5 a digit number."
                                     :nl "Een rekeningnummer kan 1 to 5 cijfers lang zijn."}}
                    #"^[0-9]{1,5}$"])

(def AccountType [:enum {:title "Account types"} :ast :lia :cst :prf])
(def SummaryLevel [:and pos-int? [:>= 0] [:<= 4]])
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
                (log/error (str "account/create-account: " (me/humanize reason)))
                (:errors reason)))}))

(defn import-accounts-fixture
  []
  (let [fixture (edn/read-string (slurp "resources/fixtures/accounts.edn"))]
    (->> fixture
         (map #(create-account %))
         (filter #(:status %))
         (mapv #(:value %)))))

(comment
  "Some repl testing code"
  (create-account {:account/id "12345" :account/name "ABC" :account/type :ast})
  (count (import-accounts-fixture))
  (m/validate AccountNumber "400100")
  (m/explain AccountNumber "400100")
  (let [account {:account/id            "800100"
                 :account/name          "Turnover High VAT"
                 :account/type          :prf
                 :account/summary-level 0}]
    (m/validate Account account)
    (m/explain Account account))
  "")


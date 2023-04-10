;; Implementation for using account with postgresql as database
(ns ixn.schema.account-pg
  (:require [clojure.tools.reader.edn :as edn]
            [ixn.schema.account :refer [Account AccountNumber create-account]]
            [malli.core :as m]
            [next.jdbc :as jdbc]
            [next.jdbc.sql :as sql]
            [ixn.rdbms.db :refer [datasource]]))

(def account-table
  ["create table account
   (
     id            char(5)      not null primary key,
     name          varchar(127) not null,
     type          varchar(32),
     summary_level integer)])"

   "alter table account
        owner to postgres"

   "create index account_name__index
        on account (name)"])


(defn import-accounts-fixture
  "Import a default account schema."
  []
  (let [fixture (edn/read-string (slurp "resources/fixtures/accounts.edn"))]
    (->> fixture
         (map #(create-account %))
         (filter #(:status %))
         (mapv (fn [k]
                 (let [{:account/keys [id name type summary-level]} (:value k)]
                   {:id            id
                    :name          name
                    :type          (case type
                                     :ast "Asset"
                                     :lia "Liability"
                                     :cst "Expense/Costs/Dividend"
                                     :prf "Income/Revenue"
                                     :ety "Equity/Capital")
                    :summary_level summary-level})))
         (sql/insert-multi! datasource "account")
         vec)))

(defn refresh-account-schema [drop]
  (when drop
    (jdbc/execute! datasource ["drop table account"]))
  (jdbc/execute! datasource account-table)
  (import-accounts-fixture))

;; Functions
(defn fetch-accounts-by-summary-level
  [lvl]
  (sql/query datasource [(format "select * from account where summary_level = %d order by id" lvl)]))

(defn fetch-accounts []
  (fetch-accounts-by-summary-level 0))

(comment
  (count (fetch-accounts)))


(comment
  (fetch-accounts-by-summary-level 0))

(defn fetch-account-by-id
  "Fetch an account by id"
  [id]
  (sql/query datasource [(format "select * from account where id = '%s'" (str id))]))

(comment
  (time (fetch-account-by-id "80100")))

(defn pull-account-by-id
  [id]
  (fetch-account-by-id id))

(comment
  (pull-account-by-id "12010"))

(defn pull-all-accounts
  "return all accounts with summary level 0"
  [{:keys [sort-on order limit offset summary-level]}]
  (let [my-limit         (if (and limit (> limit 0)) limit 10)
        my-offset        (if offset offset 0)
        my-summary-level (if summary-level summary-level 0)
        q                (format "select * from account where summary_level = %d order by %s %s limit %d offset %d"
                                 my-summary-level (name sort-on) (name order) my-limit my-offset)]
    (sql/query datasource [q])))

(comment
  (pull-all-accounts {:sort-on :account/id :order :asc :limit 100 :offset 0 :summary-level 3}))

(defn fetch-accounts-by-type
  "Fetch an account by account-type"
  [tp]
  (let [my-type (case tp
                  :ast "Asset"
                  :lia "Liability"
                  :cst "Expense/Costs/Dividend"
                  :prf "Income/Revenue"
                  :ety "Equity/Capital")]
    (sql/query datasource [(format "select * from account where type = '%s'" my-type)])))

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

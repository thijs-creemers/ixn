(ns ixn.rdbms.playground
  (:require
    [clojure.tools.reader.edn :as edn]
    [next.jdbc :as jdbc]
    [next.jdbc.sql :as sql]
    [ixn.schema.account :refer [AccountNumber AccountType Account create-account]]))

(def db-spec
  {:dbtype   "postgresql"
   :host     "localhost"
   :port     5432
   :dbname   "ixn-db"
   :user     "postgres"
   :password "XMJeQns0QGBfDhIxiDAp"})

(def datasource (jdbc/get-datasource db-spec))

(def account-table
  ["create table account (
    id char(5) PRIMARY KEY,
    name varchar(127),
    type varchar(32),
    summary_level int)"])



(comment
  (create-account {:account/id "12000" :account/name "een rekening" :account/type :ast}))

(defn rec [{:accounts/keys [id name type summary-level]}]
  {:id            id
   :name          name
   :type          type
   :summary-level summary-level})


(defn import-accounts-fixture
  []
  (let [fixture (edn/read-string (slurp "resources/fixtures/accounts.edn"))]
    (->> fixture
         (map #(create-account %))
         (filter #(:status %))
         (mapv (fn [k]
                 (let [{:account/keys [id name type summary-level]} (:value k)]
                   {:id id
                    :name name
                    :type (case type
                            :ast "Asset"
                            :lia "Liability"
                            :cst "Expense/Costs/Dividend"
                            :prf "Income/Revenue"
                            :ety "Equity/Capital")
                    :summary_level summary-level})))
         (sql/insert-multi! datasource "account")
         vec)))

(defn refresh-account-schema []
  (jdbc/execute! datasource ["drop table account"])
  (jdbc/execute! datasource account-table)
  (import-accounts-fixture))


(comment
  (refresh-account-schema)
  (jdbc/execute! datasource ["create table address (
    id bigint GENERATED ALWAYS AS IDENTITY (CACHE 200) PRIMARY KEY,
    name varchar(32),
    email varchar(255))"])

  (sql/get-by-id datasource "account" "80100")
  (sql/query datasource ["select * from account where summary_level = 0 order by id"])
  (jdbc/execute! datasource ["drop table account"])
  (jdbc/execute! datasource ["drop table address"])
  (jdbc/execute! datasource ["insert into address (name, email) values ('John Doe', 'jodo@johndoe.com')"])
  (jdbc/execute! datasource ["insert into address (name, email) values ('Jane Doe', 'jad@johndoe.com')"])
  (jdbc/execute! datasource ["insert into address (name, email) values ('Jet Doe', 'jedo@johndoe.com')"])
  (:address/name (first (jdbc/execute! datasource ["select * from address"])))
  ())
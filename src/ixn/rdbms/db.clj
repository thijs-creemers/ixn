(ns ixn.rdbms.db
  (:require [next.jdbc :as jdbc]))

(defonce db-spec
  {:dbtype   "postgresql"
   :host     "localhost"
   :port     5432
   :dbname   "ixn"
   :user     "postgres"
   :password "ixnsecret"})

(defonce datasource (jdbc/get-datasource db-spec))


(defn import-default-fixtures []
  {})
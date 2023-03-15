(ns ixn.rdbms.playground
  (:require [next.jdbc :as jdbc]))

(def db-spec
  {:dbtype   "postgresql"
   :host     "localhost"
   :port     5432
   :dbname   "ixn-db"
   :user     "postgres"
   :password "XMJeQns0QGBfDhIxiDAp"})



(def ds (jdbc/get-datasource db-spec))

(def account-table
  ["create table account (
    id bigint GENERATED ALWAYS AS IDENTITY (CACHE 200) PRIMARY KEY,
    name varchar(32),
    type varchar(32),
    summary_level int)"])


(comment
  (jdbc/execute! ds ["create table address (
    id bigint GENERATED ALWAYS AS IDENTITY (CACHE 200) PRIMARY KEY,
    name varchar(32),
    email varchar(255))"])

  (jdbc/execute! ds account-table)
  (jdbc/execute! ds ["drop table address"])
  (jdbc/execute! ds ["insert into address (name, email) values ('John Doe', 'jodo@johndoe.com')"])
  (jdbc/execute! ds ["insert into address (name, email) values ('Jane Doe', 'jad@johndoe.com')"])
  (jdbc/execute! ds ["insert into address (name, email) values ('Jet Doe', 'jedo@johndoe.com')"])
  (:address/name (first (jdbc/execute! ds ["select * from address"])))
  ())
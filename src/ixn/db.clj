(ns ixn.db
  (:require
   [clojure.java.io :as io]
   [xtdb.api :as xt]
   [ixn.utils :refer [uuid]]))

(defn start-xtdb! []
  (letfn [(kv-store [dir]
            {:kv-store {:xtdb/module 'xtdb.rocksdb/->kv-store
                        :db-dir      (io/file dir)
                        :sync?       true}})]
    (xt/start-node
     {:xtdb/tx-log         (kv-store "data/dev/tx-log")
      :xtdb/document-store (kv-store "data/dev/doc-store")
      :xtdb/index-store    (kv-store "data/dev/index-store")})))

(def xtdb-node
  (try
    (let [xtdb (start-xtdb!)]
      (prn "XTDB-Node started")
      xtdb)
    (catch clojure.lang.ExceptionInfo e
      (prn "Caught " e))))


(defn stop-xtdb! []
  (.close xtdb-node))


(defn prepare-list-of-maps
  "Get a list of maps and prepare them to be stored in XTDB DB"
  [data]
  (mapv (fn [v]
          [:xtdb.tx/put (assoc-in v [:xtdb.db/id] (uuid))]) data))

(defn transact!
  "Store a list of maps in XTDB"
  [list-of-maps]
  (xt/submit-tx xtdb-node
                (->> list-of-maps
                     prepare-list-of-maps
                     vec)))

(comment
  (start-xtdb!)

  xtdb-node
  (xt/submit-tx xtdb-node [[::xt/put {:xt/id "6"
                                      :user/name "Nienke"
                                      :user/last-name "Creemers"}]])
  (xt/q (xt/db xtdb-node) '{:find  [e fn ln]
                            :where [[e :user/name fn]
                                    [e :user/last-name ln]]})
  (count (xt/q
          (xt/db xtdb-node)
          '{:find  [(pull ?account [*])]
            :where [[?account :account/summary-level 0]
                    [?account :account/type :ast]]})))

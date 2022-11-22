(ns ixn.db
  (:require
   [clojure.java.io :as io]
   [clojure.tools.logging :as log]
   [xtdb.api :as xt]
   [ixn.utils :refer [uuid]]))

(defonce xtdb-state (atom {:started false :xtdb nil}))

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
  (if (:started @xtdb-state)
    (:xtdb @xtdb-state)
    (try
      (reset! xtdb-state {:started true :xtdb (start-xtdb!)})
      (prn "XTDB-Node started")
      (:xtdb @xtdb-state)
      (catch clojure.lang.ExceptionInfo e
        (log/error (str "Caught " e))))))

(defn stop-xtdb! []
  (.close xtdb-node)
  (reset! xtdb-state {:started false :xtdb nil}))

(defn prepare-list-of-maps
  "Get a list of maps and prepare them to be stored in XTDB DB"
  [data]
  (mapv (fn [v]
          [::xt/put (assoc-in v [:xt/id] (uuid))]) data)) ;:xtdb.tx/put

(defn transact!
  "Store a list of maps in XTDB"
  [list-of-maps]
  (xt/submit-tx
   xtdb-node
   (->> list-of-maps
        prepare-list-of-maps)))

(comment
  (start-xtdb!)

  xtdb-node
  (xt/submit-tx xtdb-node [[::xt/put {:xt/id "6"
                                      :user/name "Nienke"
                                      :user/last-name "Creemers"}]])
  (xt/q (xt/db xtdb-node) '{:find  [e fn ln]
                            :where [[e :user/name fn]
                                    [e :user/last-name ln]]})
  (time (xt/q
         (xt/db xtdb-node)
         '{:find [(pull ?invoice [*])]
           :where [[?invoice :transaction/sub-admin "123"]
                   [?invoice :transaction/id #uuid"0edf6eee-acbb-4d89-93ca-8fd800f463ee"]]}))

  (count (xt/q
          (xt/db xtdb-node)
          '{:find  [(pull ?account [*])]
            :where [[?account :account/summary-level 0]
                    [?account :account/type :ast]]}))

  (let [q '{:find [(pull ?e [:object_type/id :name
                             {:property_types [:name :data_type :unit :visible]}])]
            :where [[?e :name "%s"]
                    [?e :data_type "enumeration"]]}]
    (prn q))
  ...)

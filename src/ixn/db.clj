(ns ixn.db
  (:require
    [clojure.java.io :as io]
    [integrant.core :as ig]
    [ixn.utils :refer [uuid]]
    [xtdb.api :as xt]
    [ixn.state :refer [system]]))


(defmethod ig/init-key
  :database
  [_ {:xtdb/keys [module sync? tx-log document-store index-store]}]
  ;; TODO: figure out how to read the 'module.
  (letfn [(kv-store [dir]
            {:kv-store {:xtdb/module 'xtdb.rocksdb/->kv-store
                        :db-dir      (io/file dir)
                        :sync?       sync?}})]
    (let [res (xt/start-node
                {:xtdb/tx-log         (kv-store tx-log)
                 :xtdb/document-store (kv-store document-store)
                 :xtdb/index-store    (kv-store index-store)})]
      (prn "Started XTDB server!")
      (swap! system assoc :database res))))


(def xtdb-node (:database (:database @system)))

(defmethod ig/halt-key! :database [_ xtdb-node]
  (.close xtdb-node))

(defn prepare-list-of-maps
  "Get a list of maps and prepare them to be stored in XTDB DB"
  [data]
  (mapv (fn [v]
          [::xt/put (assoc-in v [:xt/id] (uuid))])
        data))   ;:xtdb.tx/put

(defn transact!
  "Store a list of maps in XTDB"
  [list-of-maps]
  (xt/submit-tx
    xtdb-node
    (->> list-of-maps
         prepare-list-of-maps)))

(comment
  ;; Some manual tests to see if database works.
  (xt/submit-tx xtdb-node [[::xt/put {:xt/id          "6"
                                      :user/name      "Nienke"
                                      :user/last-name "Creemers"}]])
  (xt/q (xt/db xtdb-node) '{:find  [e fn ln]
                            :where [[e :user/name fn]
                                    [e :user/last-name ln]]})
  (time (xt/q
          (xt/db xtdb-node)
          '{:find  [(pull ?invoice [*])]
            :where [[?invoice :transaction/sub-admin "123"]
                    [?invoice :transaction/id #uuid"0edf6eee-acbb-4d89-93ca-8fd800f463ee"]]}))

  (count (xt/q
           (xt/db xtdb-node)
           '{:find  [(pull ?account [*])]
             :where [[?account :account/summary-level 0]
                     [?account :account/type :ast]]}))
  ...)

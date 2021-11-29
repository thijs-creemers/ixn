(ns ixn.db
  (:require
    [clojure.java.io :as io]
    [crux.api :as crux]
    [ixn.utils :refer [uuid]]))

(defn start-crux! []
  (letfn [(kv-store [dir]
            {:kv-store {:crux/module 'crux.rocksdb/->kv-store
                        :db-dir      (io/file dir)
                        :sync?       true}})]
    (crux/start-node
      {:crux/tx-log         (kv-store "data/dev/tx-log")
       :crux/document-store (kv-store "data/dev/doc-store")
       :crux/index-store    (kv-store "data/dev/index-store")})))

(def crux-node
  (try
    (let [crx (start-crux!)]
      (prn "Crux-Node started")
      crx)
    (catch clojure.lang.ExceptionInfo e
      (prn "Caught " e))))


(defn stop-crux! []
  (.close crux-node))

(defn prepare-list-of-maps
  "Get a list of maps and prepare them to be stored in crux DB"
  [data]
  (mapv (fn [v]
          [:crux.tx/put (assoc-in v [:crux.db/id] (uuid))]) data))

(defn transact!
  "Store a list of maps in crux"
  [list-of-maps]
  (crux/submit-tx crux-node
                  (->> list-of-maps
                       prepare-list-of-maps
                       vec))

 (comment
   crux-node
   (crux/submit-tx crux-node [[:crux.tx/put {:crux.db/id "6" :user/name "Nienke" :user/last-name "Creemers"}]
                              [:crux.tx/put {:crux.db/id "7" :user/name "Luis" :user/last-name "Janssen"}]])
   (crux/q (crux/db crux-node) '{:find  [e fn ln]
                                 :where [[e :user/name fn]
                                         [e :user/last-name ln]]})
   (crux/q
     (crux/db crux-node)
     '{:find  [(pull ?account [*])]
       :where [[?account :account/summary-level 1]]})
   ,))

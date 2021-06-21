(ns ixn.db
  (:require
            [clojure.java.io :as io]
            [crux.api :as crux]
            [ixn.utils :refer [uuid]]))

(declare crux-node)

(defn start-crux! []
  (when (not crux-node)
    (letfn [(kv-store [dir]
              {:kv-store {:crux/module 'crux.rocksdb/->kv-store
                          :db-dir      (io/file dir)
                          :sync?       true}})]
      (crux/start-node
        {:crux/tx-log              (kv-store "data/dev/tx-log")
         :crux/document-store      (kv-store "data/dev/doc-store")
         :crux/index-store         (kv-store "data/dev/index-store")}))))

(def crux-node (start-crux!))

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
         vec)))

(comment
  crux-node
  (crux/submit-tx crux-node [[:crux.tx/put {:crux.db/id "6" :user/name "Nienke"}]
                             [:crux.tx/put {:crux.db/id "7" :user/name "Luis"}]])
  (crux/q (crux/db crux-node) '{:find [e]
                                :where [[e :user/name "Thijs"]]})
  (crux/q
    (crux/db crux-node)
    '{:find [(pull ?account [*])]
      :where [[?account :account/summary-level 1]]})

  ,)

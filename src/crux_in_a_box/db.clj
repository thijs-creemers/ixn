(ns crux-in-a-box.db
  (:require
            [clojure.java.io :as io]
            [crux.api :as crux]))

(defn start-crux! []
  (letfn [(kv-store [dir]
            {:kv-store {:crux/module 'crux.rocksdb/->kv-store
                        :db-dir      (io/file dir)
                        :sync?       true}})]
    (crux/start-node
     {:crux/tx-log              (kv-store "data/dev/tx-log")
      :crux/document-store      (kv-store "data/dev/doc-store")
      :crux/index-store         (kv-store "data/dev/index-store")})))

;(def crux-node (start-crux!))
;
;(defn stop-crux! []
;  (.close crux-node)) ()

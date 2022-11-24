(ns ixn.db
  (:require
    [clojure.java.io :as io]
    [clojure.tools.logging :as log]
    [integrant.core :as ig]
    [ixn.utils :refer [uuid]]
    [xtdb.api :as xt]))

(def config
  (get-in (ig/read-string (slurp "resources/config.edn"))
          [:system]))

(defmethod ig/init-key :database [_ {:keys [handler] :as opts}]
  (let [{:xtdb/keys [module sync? tx-log document-store index-store]} opts]
    (letfn [(kv-store [dir]
              {:kv-store {:xtdb/module 'xtdb.rocksdb/->kv-store
                          :db-dir      (io/file dir)
                          :sync?       sync?}})]
      (xt/start-node
        {:xtdb/tx-log         (kv-store tx-log)
         :xtdb/document-store (kv-store document-store)
         :xtdb/index-store    (kv-store index-store)}))))

(def database (ig/init config [:database]))
(def xtdb-node (:database database))

(defmethod ig/halt-key! :database [_ xtdb-node]
  (.close xtdb-node))

(defn prepare-list-of-maps
  "Get a list of maps and prepare them to be stored in XTDB DB"
  [data]
  (mapv (fn [v]
          [::xt/put (assoc-in v [:xt/id] (uuid))]) data))   ;:xtdb.tx/put

(defn transact!
  "Store a list of maps in XTDB"
  [list-of-maps]
  (xt/submit-tx
    xtdb-node
    (->> list-of-maps
         prepare-list-of-maps)))

(comment
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

  (let [q '{:find  [(pull ?e [:object_type/id :name
                              {:property_types [:name :data_type :unit :visible]}])]
            :where [[?e :name "%s"]
                    [?e :data_type "enumeration"]]}]
    (prn q))
  ...)

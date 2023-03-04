(ns ixn.db
  (:require
    [integrant.core :as ig]
    [clojure.java.io :as io]
    [clojure.tools.logging :as log]
    [ixn.state :refer [system]]
    [ixn.utils :refer [uuid]]
    [xtdb.api :as xt]))

(defmethod ig/init-key
  :database
  [_ {:xtdb/keys [module sync? tx-log document-store index-store]}]
  ;; TODO: figure out how to read the 'module.
  (log/debug "param module:" module)
  (letfn [(kv-store [dir]
            {:kv-store {:xtdb/module 'xtdb.rocksdb/->kv-store
                        :db-dir      (io/file dir)
                        :sync?       sync?}})]
    (let [res (xt/start-node
                {:xtdb/tx-log         (kv-store tx-log)
                 :xtdb/document-store (kv-store document-store)
                 :xtdb/index-store    (kv-store index-store)})]
      (log/info "Started XTDB server!")
      (swap! system assoc :database res))))

(defmethod ig/halt-key! :database [_]
  (.close (:database (:database @system))))

(defn prepare-list-of-maps
  "Get a list of maps and prepare them to be stored in XTDB DB"
  [data]
  (mapv (fn [v]
          [::xt/put (assoc-in v [:xt/id] (uuid))])
        data))                                              ;:xtdb.tx/put

(defn transact!
  "Store a list of maps in XTDB"
  [list-of-maps]
  (xt/submit-tx
    (:database (:database @system))
    (->> list-of-maps
         prepare-list-of-maps)))

(defn num-of-accounts []
  (count (xt/q
           (xt/db (:database (:database @system)))
           '{:find  [(pull ?account [*])]
             :where [[?account :account/summary-level 2]
                     [?account :account/type :ast]]})))


(comment
  ;; Some manual tests to see if database works.
  (xt/submit-tx (:database (:database @system)) [[::xt/put {:xt/id          "6"
                                                            :user/last-name "Creemers"}]])
  (xt/q (xt/db (:database (:database @system))) '{:find  [e fn ln]
                                                  :where [[e :user/name fn]
                                                          [e :user/last-name ln]]})
  (time (xt/q
          (xt/db (:database (:database @system)))
          '{:find  [(pull ?invoice [*])]
            :where [[?invoice :transaction/sub-admin "123"]
                    [?invoice :transaction/id #uuid"0edf6eee-acbb-4d89-93ca-8fd800f463ee"]]}))

  ;; :ast :lia :cst :prf
  (count (xt/q
           (xt/db (:database (:database @system)))
           '{:find  [(pull ?account [*])]
             :where [[?account :account/summary-level 2]
                     [?account :account/type :ast]]}))

  (num-of-accounts)
  ...)

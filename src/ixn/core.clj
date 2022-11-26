(ns ixn.core
  (:require
   [integrant.core :as ig]
   [ixn.db :as db]
   [ixn.main :as main]
   [ixn.state :refer [system]]
   [ixn.db]
   [clojure.tools.logging :as log])
  (:gen-class))

(def config
  (get-in (ig/read-string (slurp "resources/config.edn")) [:system]))

(defmethod ig/init-key :logging [_ opts]
  (swap! system :logging (:logging opts))
  (log/info "Configured logging setup"))

(defn stop-app []
  (some-> (deref system) (ig/halt!))
  (shutdown-agents))

(defn start-app []
  (->> config
       (ig/prep)
       (ig/init)
       (reset! system))
  (log/info "Started IXN accounting"))

(defn -main [& _]
  (start-app))

(comment
  (start-app)
  (stop-app)
  @system
  ...)


(ns ixn.core
  (:require
   [integrant.core :as ig]
   [ixn.db :refer [num-of-accounts]]
   [clojure.tools.logging :as log]
   [ixn.main]
   [ixn.state :refer [system]])
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
  ixn.main/routes
  (start-app)
  (stop-app)
  (num-of-accounts)
  (log/info "Configured logging setup")
  @system
  ...)

(ns ixn.core
  (:require
   [integrant.core :as ig]
   [ixn.db :as db]
   [ixn.main :as main]
   [ixn.state :refer [system]]
   [ixn.db])
  (:gen-class))

(def config
  (get-in (ig/read-string (slurp "resources/config.edn")) [:system]))

(defn stop-app []
  (some-> (deref system) (ig/halt!))
  (shutdown-agents))

(defn start-app []
  (->> config
       (ig/prep)
       (ig/init)
       (reset! system))
  (prn "started"))

(defn -main [& _]
  (start-app))

(comment
  (start-app)
  (stop-app)
  @system)


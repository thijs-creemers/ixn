(ns ixn.core
  (:require
    [integrant.core :as ig])
  (:gen-class))

(defonce system (atom nil))

(def config
  (get-in (ig/read-string (slurp "resources/config.edn")) [:system]))

(defn stop-app []
  (some-> (deref system) (ig/halt!))
  (shutdown-agents))

(defn start-app []
  (->> config
       (ig/prep)
       (ig/init)
       (reset! system)))

(defn -main [& _]
  (start-app))

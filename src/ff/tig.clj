(ns ff.tig
  (:require [integrant.core :as ig]
            [io.pedestal.http :as http]))

;(def config
;  {:adapter/jetty {:port 8080, :handler (ig/ref :handler/greet)}
;   :handler/greet {:name "Alice"}))

(def config
  (ig/read-string (slurp "src/ff/config1.edn")))


(def routes #{})

(defmethod ig/init-key :adapter/jetty [_ {:keys [handler] :as opts}]
  (http/start (-> {::http/routes         routes
                   ::http/type           :jetty
                   ::http/join?          false
                   ::http/port           3300}
                  http/default-interceptors
                  http/create-server)))

(defmethod ig/init-key :handler/greet [_ {:keys [name]}]
  (fn [_]
    {:status 200
     :body   (str "Hello " name)}))

(comment
  (ig/init config)

  ..)
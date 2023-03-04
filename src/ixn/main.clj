(ns ixn.main
  (:require
    [clojure.tools.logging :as log]
    [integrant.core :as ig]
    [io.pedestal.http :as http]
    [io.pedestal.http.route :as route]
    [io.pedestal.test]
    [ixn.financial.views.accounts :refer [accounts-list accounts-refresh]]
    [ixn.financial.views.accounts-api :as accounts-api]
    [ixn.frontend.core :refer [htmx]]
    [ixn.state :refer [system]]
    [clojure.set :refer [union]]))


(def routes
  (route/expand-routes
   (union #{["/accounts-list" :get accounts-list :route-name :accounts-list]
            ["/accounts-refresh" :get accounts-refresh :route-name :accounts-refresh]
            ["/htmx.js.min" :get htmx :route-name :htmx]}
          accounts-api/routes)))

(defmethod ig/init-key :web/server [_ {:keys [handler] :as opts}]
  (let [res (-> opts
                (assoc :io.pedestal.http/routes routes)
                http/default-interceptors
                ;; extra interceptors are added
                http/create-server
                http/start)]
    (log/info "Started HTTP Server")
    (swap! system assoc-in [:http-server] res)
    res))

(def http-server
  {:web/server (get-in @system [:web/server])})

(defmethod ig/halt-key! :web/server [_ http-server]
  (http/stop http-server))

;; test helpers
(defn test-request [verb url]
  (io.pedestal.test/response-for (::http/service-fn http-server) verb url))

(comment
  (htmx 1)
  (accounts-list 1)

  ())

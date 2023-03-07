(ns ixn.main
  (:require
    [clojure.set :refer [union]]
    [clojure.tools.logging :as log]
    [integrant.core :as ig]
    [io.pedestal.http :as http]
    [io.pedestal.http.route :as route]
    [io.pedestal.test]
    [ixn.authentication.views :as auth-views]
    [ixn.financial.views.accounts :as accounts]
    [ixn.financial.views.accounts-api :as accounts-api]
    [ixn.financial.views.accounts-payable :as accounts-payable]
    [ixn.financial.views.accounts-payable-api :as accounts-payable-api]
    [ixn.frontend.core :refer [htmx]]
    [ixn.state :refer [system]]))


(def routes
  (route/expand-routes
    (union #{["/htmx.js.min" :get htmx :route-name :htmx]}
           auth-views/routes
           accounts/routes
           accounts-api/routes
           accounts-payable/routes
           accounts-payable-api/routes)))

(defmethod ig/init-key :web/server [_ {:keys [handler] :as opts}]
  (let [res (-> opts
                (assoc :io.pedestal.http/routes routes)
                http/default-interceptors
                ;; extra interceptors are added
                http/create-server
                http/start)]
    (log/info (str "Started HTTP Server on host '" (:io.pedestal.http/host opts) "' with port: '" (:io.pedestal.http/port opts) "'"))
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
  (http/start (get-in @system [:web/server]))

  ())

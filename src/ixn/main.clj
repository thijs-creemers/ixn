(ns ixn.main
  (:require
   [io.pedestal.http :as http]
   [io.pedestal.http.content-negotiation :as cn]
   [io.pedestal.http.route :as route]
   [io.pedestal.test]
   [jsonista.core :as json]
   [integrant.core :as ig]
   [ixn.state :refer [system]]
   [ixn.financial.frontend.accounts :refer [get-accounts accounts-refresh accounts-list]]
   [clojure.tools.logging :as log]))

(def supported-types ["text/html" "application/edn" "application/json" "text/plain"])
(def content-negociation-interceptor (cn/negotiate-content supported-types))

(defn ->json [edn-value]
  (-> edn-value
      (json/write-value-as-bytes json/default-object-mapper)
      (json/read-value json/default-object-mapper)))

(def coerce-body
  {:name  ::coerce-body
   :leave (fn [context]
            (let [accepted         (get-in context [:request :accept :field] "text/plain")
                  response         (get context :response)
                  body             (get response :body)
                  coerced-body     (case accepted
                                     "text/html" body
                                     "text/plain" body
                                     "text/css" body
                                     "application/edn" (pr-str body)
                                     "application/json" (->json body)
                                     "application/javascript" body)
                  updated-response (assoc response
                                          :headers {"Content-Type" accepted}
                                          :body coerced-body)]
              (assoc context :response updated-response)))})


(defn htmx [_]
  {:status  200
   :body    (slurp "resources/public/js/htmx.js.min")
   :headers {"Content-Type" "application/javascript"}})

(def routes
  (route/expand-routes
   #{["/api/v1/accounts" :get [coerce-body content-negociation-interceptor get-accounts] :route-name :accounts]
     ["/accounts-list" :get accounts-list :route-name :accounts-list]
     ["/accounts-refresh" :get accounts-refresh :route-name :accounts-refresh]
     ["/htmx.js.min" :get htmx :route-name :htmx]}))

;; Setup
(defmethod ig/init-key :web/server [_ {:keys [handler] :as opts}]
  (let [res (-> opts
                (assoc :io.pedestal.http/routes routes)
                http/default-interceptors
                http/create-server
                ;; extra interceptors are added
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

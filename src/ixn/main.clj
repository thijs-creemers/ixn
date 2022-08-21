(ns ixn.main
  (:require
    ;; [integrant.core :as ig]
   [io.pedestal.http :as http]
   [io.pedestal.http.content-negotiation :as conneg]
   [io.pedestal.http.route :as route]
   [io.pedestal.test]
   [ixn.financial.chart-of-accounts :as coa]
   [ixn.schema.account :refer [pull-all-accounts]]
   [jsonista.core :as json]
   [rum.core :as rum]))

(def supported-types ["text/html" "application/edn" "application/json" "text/plain"])
(def content-negociation-interceptor (conneg/negotiate-content supported-types))

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

(defn html-doc
  [title body-content]
  [:html
   [:head
    [:title title]
    [:meta {:name "viewport" :content "width=device-width, initial-scale=1" :charset "utf-8"}]
    [:link {:rel "stylesheet" :href "https://unpkg.com/tachyons/css/tachyons.min.css"}]
    [:script {:src "/htmx.js.min" :defer "true" :type "application/javascript"}]]
   [:body.bg-gray-30.black-70.pa4.avenir
    [:div.fl.w-100
     ;[:div.fl.w-20 "FF"]
     [:div.fl.w-80 body-content]]]])

(defn get-accounts [_]
  (let [all-accounts (pull-all-accounts {:sort-on :account/id :order :asc})]
    {:status 200
     :body   all-accounts}))

(defn accounts-refresh [_]
  (let [body (rum/render-html (coa/overview))]
    {:status  200
     :body    body
     :headers {"Content-Type" "text/html"}}))

(defn accounts-list [_]
  (let [body (rum/render-html (html-doc "Accounts list" (coa/overview)))]
    {:status  200
     :body    body
     :headers {"Content-Type" "text/html"}}))

(defn htmx [_]
  {:status 200
   :body (slurp "resources/public/js/htmx.js.min")
   :headers {"Content-Type" "application/javascript"}})

(def routes
  (route/expand-routes
   #{["/api/v1/accounts" :get [coerce-body content-negociation-interceptor get-accounts] :route-name :accounts]
     ["/accounts-list" :get accounts-list :route-name :accounts-list]
     ["/accounts-refresh" :get accounts-refresh :route-name :accounts-refresh]
     ["/htmx.js.min" :get htmx :route-name :htmx]}))

(def service-map
  {::http/routes         routes
   ;; Resources will be served from the resource directory's `public`
   ;; sub directory.
   ::http/secure-headers {:content-security-policy-settings {:object-src "http://localhost:3300/*"}}
   ::http/resource-path  "/public"
   ::http/type           :jetty
   ::http/join?          false
   ::http/port           3300})

(defn start []
  (http/start (-> service-map
                  http/default-interceptors
                  http/create-server)))

;; For interactive development
(defonce server (atom nil))

(defn start-dev []
  (reset! server (-> service-map
                     http/default-interceptors
                     http/create-server
                     ;; extra interceptors are added
                     http/start)))

(defn stop-dev []
  (http/stop @server))

(defn restart []
  (stop-dev)
  (start-dev))

;; test helpers
(defn test-request [verb url]
  (io.pedestal.test/response-for (::http/service-fn @server) verb url))

(comment
  (test-request :get "/api/v1/accounts")
  (test-request :get "/accounts-list")
  (restart)
  (+ 1 1)

  ())

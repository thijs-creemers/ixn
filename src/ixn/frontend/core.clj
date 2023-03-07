(ns ixn.frontend.core
  (:require [ixn.utils :refer [->json]]
            [io.pedestal.http.content-negotiation :as cn]))


(def supported-types ["text/html" "application/edn" "application/json" "application/xml" "text/plain"])
(def content-negociation-interceptor (cn/negotiate-content supported-types))

(defn htmx [_]
  {:status  200
   :body    (slurp "resources/public/js/htmx.js.min")
   :headers {"Content-Type" "application/javascript"}})

(defn html-doc
  "Return the basic HTML doc."
  [title body-content]
  [:html
   [:head
    [:title title]
    [:meta {:name "viewport" :content "width=device-width, initial-scale=1" :charset "utf-8"}]
    [:link {:rel "stylesheet" :href "https://unpkg.com/tachyons/css/tachyons.min.css"}]
    [:script {:src "/htmx.js.min" :defer "true" :type "application/javascript"}]]
   [:body.bg-gray-30.black-70.pa4.avenir
    [:div.fl.w-100
     [:div.fl.w-20 "menu"]
     [:div.fl.w-80 body-content]]]])

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

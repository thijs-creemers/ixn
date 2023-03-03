(ns ixn.frontend.core)

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
     [:div.fl.w-80 body-content]]]])


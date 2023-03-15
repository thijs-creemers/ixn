(ns ixn.authentication.views
  (:require [ixn.settings :refer [content-security-policy]]
            [rum.core :as rum]))

(defn html-doc
  "Return the basic HTML doc."
  [title body-content]
  [:html
   [:head
    [:title title]
    [:meta {:name "viewport" :content "width=device-width, initial-scale=1" :charset "utf-8"}]
    [:link {:rel "stylesheet" :href "https://unpkg.com/tachyons/css/tachyons.min.css"}]
    [:script {:src "/htmx.js.min" :type "application/javascript"}]]
   [:body.bg-gray-30.black-70.pa4.avenir
    [:div.fl.w-100 body-content]]])

(rum/defc login-form []
  [:form.measure.center {:method "POST"}
   [:fieldset#sign_up.ba.b--transparent.ph0.mh0
    [:legend.f4.fw6.ph0.mh0 "Sign In"]
    [:div.mt3
     [:label.db.fw6.lh-copy.f6 {:for "email-address"} "Email"]
     [:input#email-address.pa2.input-reset.ba.bg-transparent.hover-bg-black.hover-white.w-100 {:type "email" :name "email-address"}]]
    [:div.mv3
     [:label.db.fw6.lh-copy.f6 {:for "password"} "Password"]
     [:input#password.b.pa2.input-reset.ba.bg-transparent.hover-bg-black.hover-white.w-100 {:type "password" :name "password"}]]
    [:label.pa0.ma0.lh-copy.f6.pointer [:input {:type "checkbox"}] "Remember me"]]
   [:div {:class ""}
    [:input.b.ph3.pv2.input-reset.ba.b--black.bg-transparent.grow.pointer.f6.dib {:type "submit" :value "Sign in"}]]
   [:div.lh-copy.mt3
    [:a.f6.link.dim.black.db {:href "#0"} "Sign up"]
    [:a.f6.link.dim.black.db {:href "#0"} "Forgot your password?"]]])

(defn sign-in [_]
  (let [body (rum/render-html (html-doc "Sign In" (login-form)))]
    {:status  200
     :body    body
     :headers {"Content-Type" "text/html" "Content-Security-Policy" content-security-policy}}))

(def routes
  #{["/sign-in" :get sign-in :route-name :sign-in]})
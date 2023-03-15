(ns ixn.financial.views.balance
  (:require
    [ixn.financial.balance.transaction :refer [fetch-all-transactions]]
    [ixn.frontend.core :refer [html-doc]]
    [ixn.frontend.style :refer [caption-row data-row table-row]]
    [ixn.schema.money :refer [mformat]]
    [ixn.settings :refer [content-security-policy]]
    [ixn.utils :refer [format-date parse-integer]]

    [rum.core :as rum]))


(rum/defc balance-caption
  []
  [:div {:class caption-row}
   [:div.w-20 {:class data-row} "Id"]
   [:div.w-10 {:class data-row} "Journal"]
   [:div.w-10 {:class data-row} "Account"]
   [:div.w-10 {:class data-row} "Date"]
   [:div.w-20 {:class data-row} "Description"]
   [:div.w-10 {:class data-row} "Amount"]])

(rum/defc balance-row [{:keys [id account description journal year period date amount side account-name]}]
  (let [my-amount      (if (= :credit side) (mformat amount) (str (mformat amount) "-"))
        formatted-date (format-date date)]
    [:div {:class table-row}
     [:div.w-20 {:class data-row} id]
     [:div.w-10 {:class data-row} journal]
     [:div.w-10 {:class data-row} (format "%s - %s" account account-name)]
     [:div.w-10 {:class data-row} formatted-date]
     [:div.w-20 {:class data-row} description]
     [:div.w-10 {:class data-row} my-amount]]))

(rum/defc balances
  [year period]
  (for [transaction
        (fetch-all-transactions {:sort-on :account/id
                                 :order   :asc
                                 :year    year
                                 :period  period})]
    (balance-row transaction)))

(rum/defc form-field [name helper-text optional]
  [:div.measure
   [:label.f6.b.db.mb2 {:for name} name (when optional [:span.normal.black-60 "(optional)"])]
   [:input.input-reset.ba.b--black-20.pa2.mb2.db.w-100 {:id name :type "text" :aria-describedby (str name "-desc")}]
   [:small.f6.black-60.db.mb2 {:id (str name "-desc")} helper-text]])


(rum/defc overview [year period]
  [:div
   [:h1.f1 "Balance list"]
   [:div.dt
    [:div [:h3.f3 (format "Book period: %04d - %02d" year period)]]
    (balance-caption)
    (balances year period)]])


(defn balance-list-response
  "Return a HTML page with alance list response for a booking period indicated by :year :period."
  [request]
  (let [year   (parse-integer (:year (:path-params request)) 10)
        period (parse-integer (:period (:path-params request)) 10)
        body   (rum/render-html (html-doc "Balance list" (overview year period)))]
    {:status  200
     :body    body
     :headers {"Content-Type" "text/html" "Content-Security-Policy" content-security-policy}}))

(def routes
  #{["/balance-list/:year/:period" :get balance-list-response :route-name :balance-list]})

(comment
  (balances 2023 3)
  (fetch-all-transactions {:sort-on :account/id
                           :order   :asc
                           :year    2023
                           :period  3})
  ...)
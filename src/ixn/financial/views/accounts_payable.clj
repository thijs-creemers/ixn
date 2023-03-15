(ns ixn.financial.views.accounts-payable
  (:require
    [ixn.financial.payable.transaction :refer [pull-all-purchase-invoices]]
    [ixn.frontend.core :refer [html-doc form-field]]
    [ixn.frontend.style :refer [caption-row data-row table-row]]
    [ixn.schema.money :refer [mformat]]
    [ixn.utils :refer [format-date]]
    [ixn.settings :refer [content-security-policy]]
    [rum.core :as rum]))

"
:transaction/id
:transaction/line
:transaction/date
:transaction/year
:transaction/period
:transaction/journal
:transaction/account
:transaction/description
:transaction/cost-center
:transaction/sub-admin
:transaction/amount
:transaction/side
:transaction/invoice
"

(rum/defc form []
  [:div
   [:div
    [:form.black-80
     [:fieldset.ba.b--dotted.bw1
      [:legend "Enter purchase invoice"]
      (form-field "transaction/id" "The id of the transaction" false)
      (form-field "transaction/date" "The transaction date" false)
      (form-field "transaction/year" "The transaction year" false)
      (form-field "transaction/period" "The transaction period" false)
      (form-field "transaction/description" "The transaction description" false)
      (form-field "transaction/invoice" "The invoice number for this invoice" false)
      (form-field "transaction/cost-center" "The costcenter to assign this invoice." false)]]]])


(rum/defc purchase-invoice-caption
  []
  [:div {:class caption-row}
   [:div.w-30 {:class      data-row
               :hx-trigger "click"
               ;:hx-swap "outerHTML"
               :hx-get     "/purchase-invoice-refresh?sort=:transaction/id&order=:asc"} "Transaction"]
   [:div.w-10 {:class      data-row
               :hx-trigger "click"
               ;:hx-swap "outerHTML"
               :hx-get     "/purchase-invoice-refresh?sort=:transaction/date&order=:asc"} "Date"]
   [:div.w-10 {:class      data-row
               :hx-trigger "click"
               ;:hx-swap "outerHTML"
               :hx-get     "/purchase-invoice-refresh?sort=:transaction/year&order=:asc"} "Year-Period"]
   [:div.w-10 {:class      data-row
               :hx-trigger "click"
               ;:hx-swap "outerHTML"
               :hx-get     "/purchase-invoice-refresh?sort=:transaction/sub-admin&order=:asc"} "Creditor"]
   [:div.w-10 {:class      data-row
               :hx-trigger "click"
               ;:hx-swap "outerHTML"
               :hx-get     "/purchase-invoice-refresh?sort=:transaction/invoice&order=:asc"} "Invoice"]

   [:div.w-20 {:class      data-row
               :hx-trigger "click"
               ;:hx-swap "outerHTML"
               :hx-get     "/purchase-invoice-refresh?sort=:transaction/invoice&order=:asc"} "Description"]

   [:div.w-10 {:class      (str data-row " tr")
               :hx-trigger "click"
               ;:hx-swap "outerHTML"
               :hx-get     "/purchase-invoice-refresh?sort=:transaction/amount&order=:asc"} "Amount"]])

(rum/defc purchase-invoice-row [{:transaction/keys [id date year period invoice sub-admin amount side description]}]
  (let [my-amount      (if (= :credit side) (mformat amount) (str (mformat amount) "-"))
        booking-period (str year " - " (format "%02d" period))
        formatted-date (format-date date)]
    [:div {:class table-row}
     [:div.w-30 {:class data-row} id]                       ;(first (string/split (str id) #"-"))]
     [:div.w-10 {:class data-row} formatted-date]
     [:div.w-10 {:class data-row} booking-period]
     [:div.w-10 {:class data-row} sub-admin]
     [:div.w-10 {:class data-row} (str "INV-" invoice)]
     [:div.w-20 {:class data-row} description]
     [:div.w-10 {:class (str data-row " tr")} my-amount]]))

(rum/defc purchase-invoice-table []
  (for [account (pull-all-purchase-invoices {:sort-on :transaction/id
                                             :order   :asc
                                             :limit   100
                                             :offset  0})]
    (purchase-invoice-row account)))


(defn get-purchase-invoices
  [_]
  (let [all-purchase-invoices (pull-all-purchase-invoices {:sort-on :transaction/id :order :asc})]
    {:status 200
     :body   all-purchase-invoices}))

(rum/defc overview []
  [:div
   [:h1.f1 "Purchase invoices overview"]
   [:div.dt
    ;(form)
    (purchase-invoice-caption)
    (purchase-invoice-table)]])

(defn purchase-invoice-refresh [_]
  (let [body (rum/render-html (overview))]
    {:status  200
     :body    body
     :headers {"Content-Type" "text/html" "Content-Security-Policy" content-security-policy}}))

(defn purchase-invoice-list [_]
  (let [body (rum/render-html (html-doc "Purchase invoice list" (overview)))]
    {:status  200
     :body    body
     :headers {"Content-Type" "text/html" "Content-Security-Policy" content-security-policy}}))


(def routes
  #{["/purchase-invoice-list" :get purchase-invoice-list :route-name :purchase-invoice-list]
    ["/purchase-invoice-refresh" :get purchase-invoice-refresh :route-name :purchase-invoice-refresh]})

(comment
  (purchase-invoice-list 1)
  ...)
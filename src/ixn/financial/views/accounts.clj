(ns ixn.financial.views.accounts
  (:require
    [ixn.frontend.core :refer [html-doc]]
    [ixn.schema.account :refer [pull-all-accounts]]
    [rum.core :as rum]
    [ixn.frontend.style :refer [caption-row data-row table-row]]))


(rum/defc account-caption

          []
          [:div {:class caption-row}
           [:div.w-10 {:class      data-row
                       :hx-trigger "click"
                       ;:hx-swap "outerHTML"
                       :hx-get     "/accounts-refresh?sort=:account/id&order=:asc"} "Id"]
           [:div.w-70 {:class      data-row
                       :hx-trigger "click"
                       ;:hx-swap "outerHTML"
                       :hx-get     "/accounts-refresh?sort=:account/name&order=:asc"} "Name"]
           [:div.w-10 {:class      data-row
                       :hx-trigger "click"
                       ;:hx-swap "outerHTML"
                       :hx-get     "/accounts-refresh?sort=:account/type&order=:asc"} "Type"]
           [:div.w-10 {:class      data-row
                       :hx-trigger "click"
                       ;:hx-swap "outerHTML"
                       :hx-get     "/accounts-refresh?sort=:account/summary-level&order=:asc"} "Sum level"]])

(rum/defc account-row [{:account/keys [id name type summary-level]}]
          [:div {:class table-row}
           [:div.w-10 {:class data-row} id]
           [:div.w-70 {:class data-row} name]
           [:div.w-10 {:class data-row} type]
           [:div.w-10 {:class data-row} summary-level]])

(rum/defc account-table []
          (for [account (pull-all-accounts {:sort-on :account/id
                                            :order   :asc
                                            :limit   20
                                            :offset  0})]
            (account-row account)))

(rum/defc form-field [name helper-text optional]
          [:div.measure
           [:label.f6.b.db.mb2 {:for name} name (when optional [:span.normal.black-60 "(optional)"])]
           [:input.input-reset.ba.b--black-20.pa2.mb2.db.w-100 {:id name :type "text" :aria-describedby (str name "-desc")}]
           [:small.f6.black-60.db.mb2 {:id (str name "-desc")} helper-text]])

(rum/defc form []
          [:div
           [:div
            [:form.black-80
             [:fieldset.ba.b--dotted.bw1
              [:legend "Enter account"]
              (form-field "account/id" "The id of the account" false)
              (form-field "account/name" "The name of the account" false)
              (form-field "account/type" "The type of the account" false)
              (form-field "account/summary-level" "The summary-level of the account" false)]]]])

(rum/defc overview []
          [:div
           [:h1.f1 "Accounts overview"]
           [:div.dt
            (form)
            (account-caption)
            (account-table)]])

(defn get-accounts
  [_]
  (let [all-accounts (pull-all-accounts {:sort-on :account/id :order :asc})]
    {:status 200
     :body   all-accounts}))

(defn accounts-refresh [_]
  (let [body (rum/render-html (overview))]
    {:status  200
     :body    body
     :headers {"Content-Type" "text/html"}}))

(defn accounts-list [_]
  (let [body (rum/render-html (html-doc "Accounts list" (overview)))]
    {:status  200
     :body    body
     :headers {"Content-Type" "text/html"}}))

(comment
  (accounts-list 1))
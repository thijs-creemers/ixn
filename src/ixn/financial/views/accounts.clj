(ns ixn.financial.views.accounts
  (:require
    [ixn.frontend.core :refer [form html-doc]]
    [ixn.frontend.style :refer [caption-row data-row table-row]]
    [ixn.schema.account :refer [pull-all-accounts]]
    [ixn.settings :refer [content-security-policy]]
    [rum.core :as rum]))


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

(rum/defc overview []
  [:div
   [:h1.f1 "Accounts overview"]
   [:div.dt
    (form "Enter account"
          {:account/id            {:description "The id of the account" :optional false}
           :account/name          {:description "The name of the account" :optional false}
           :account/type          {:description "The type of the account" :optional false}
           :account/summary-level {:description "The summary-level  of the account" :optional false}})
    (account-caption)
    (account-table)]])

(defn get-accounts
  [_]
  (let [all-accounts (pull-all-accounts {:sort-on :account/id :order :asc})]
    (if (= 0 (count all-accounts))
      {:status 404
       :body   "No accounts found"}
      {:status 200
       :body   all-accounts})))

(defn accounts-refresh [_]
  (let [body (rum/render-html (overview))]
    {:status  200
     :body    body
     :headers {"Content-Type" "text/html" "Content-Security-Policy" content-security-policy}}))

(defn accounts-list [_]
  (let [body (rum/render-html (html-doc "Accounts list" (overview)))]
    {:status  200
     :body    body
     :headers {"Content-Type" "text/html" "Content-Security-Policy" content-security-policy}})) ;; TODO: figure out how to set this policy.

(def routes
  #{["/accounts-list" :get accounts-list :route-name :accounts-list]
    ["/accounts-refresh" :get accounts-refresh :route-name :accounts-refresh]})

(comment
  (accounts-list 1))
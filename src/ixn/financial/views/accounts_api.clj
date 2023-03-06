(ns ixn.financial.views.accounts-api
  (:require
    [ixn.frontend.core :refer [coerce-body content-negociation-interceptor]]
    [ixn.schema.account :refer [pull-account-by-id pull-all-accounts]]
    [ixn.utils :refer [parse-integer]]))

(defn get-accounts
  [request]
  (let [limit        (parse-integer (:limit (:path-params request)) 20)
        offset       (parse-integer (:offset (:path-params request)) 0)
        all-accounts (pull-all-accounts {:sort-on :account/id :order :asc :limit limit :offset offset})]
    {:status 200
     :body   all-accounts}))

(defn get-account-by-id [request]
  (let [account (pull-account-by-id (:id (:path-params request)))]
    {:status 200
     :body   account}))

(def routes
  #{["/api/v1/accounts/:limit/:offset" :get [coerce-body content-negociation-interceptor get-accounts] :route-name :accounts]
    ["/api/v1/account/:id" :get [coerce-body content-negociation-interceptor get-account-by-id] :route-name :account-by-id]})


(comment
  (get-accounts 1)
  (pull-account-by-id "00011")
  (get-account-by-id "00011"))

;; Some curl test examples
;; "curl -i -H "Accept: application/json" -H "Content-Type: application/json" -X GET http://localhost:3300/api/v1/accounts/10/0"
;; "curl -i -H "Accept: application/json" -H "Content-Type: application/json" -X GET http://localhost:3300/api/v1/account/00011"
;; "curl -i -H "Accept: application/json" -H "Content-Type: application/json" -X GET http://localhost:3300/api/v1/account/00011"
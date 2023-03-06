(ns ixn.financial.views.accounts-payable-api
  (:require
    [ixn.frontend.core :refer [coerce-body content-negociation-interceptor]]
    [ixn.financial.payable.transaction :refer [pull-all-purchase-invoices pull-purchase-invoices-by-subadmin]]
    [ixn.utils :refer [parse-integer]]))

(defn get-purchase-invoices
  [request]
  (let [limit        (parse-integer (:limit (:path-params request)) 20)
        offset       (parse-integer (:offset (:path-params request)) 0)
        all-accounts (pull-all-purchase-invoices {:sort-on :account/id :order :asc :limit limit :offset offset})]
    {:status 200
     :body   all-accounts}))

(defn get-purchase-invoice-by-subadmin [request]
  (let [account (pull-purchase-invoices-by-subadmin (:sub-admin (:path-params request)))]
    {:status 200
     :body   account}))

(def routes
  #{["/api/v1/purchase-invoices/:limit/:offset" :get [coerce-body content-negociation-interceptor get-purchase-invoices] :route-name :purchase-invoices]
    ["/api/v1/purchase-invoice/:sub-admin" :get [coerce-body content-negociation-interceptor get-purchase-invoice-by-subadmin] :route-name :purchase-invoice-by-subadmin]})


(comment
  (get-purchase-invoices 1)
  (get-purchase-invoice-by-subadmin "222"))

;; Some curl test examples
;; "curl -i -H "Accept: application/json" -H "Content-Type: application/json" -X GET http://localhost:3300/api/v1/purchase-invoices/10/0"
;; "curl -i -H "Accept: application/json" -H "Content-Type: application/json" -X GET http://localhost:3300/api/v1/purchase-invoice/222"
;; "curl -i -H "Accept: application/edn" -H "Content-Type: application/edn" -X GET http://localhost:3300/api/v1/purchase-invoices/10/0"
;; "curl -i -H "Accept: application/edn" -H "Content-Type: application/edn" -X GET http://localhost:3300/api/v1/purchase-invoice/222"

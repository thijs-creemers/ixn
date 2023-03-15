(ns ixn.eql.playground
  (:require
    ;[clojure.string :as str]
    ; main namespaces, you are very likely to use at least these
   [com.wsscode.pathom3.connect.indexes :as pci]
   [com.wsscode.pathom3.connect.operation :as pco]
   [com.wsscode.pathom3.interface.eql :as p.eql]

    ; now some common namespaces to use
    ;[com.wsscode.pathom3.connect.built-in.resolvers :as pbir]
    ;[com.wsscode.pathom3.connect.built-in.plugins :as pbip]
    ;[com.wsscode.pathom3.interface.async.eql :as p.a.eql]
    ;[com.wsscode.pathom3.interface.smart-map :as psm]
    ;[com.wsscode.pathom3.plugin :as p.plugin]

    ; now the least common used
    ;[com.wsscode.pathom3.cache :as p.cache]
    ;[com.wsscode.pathom3.connect.foreign :as pcf]
    ;[com.wsscode.pathom3.connect.operation.transit :as pcot]
    ;[com.wsscode.pathom3.connect.planner :as pcp]
    ;[com.wsscode.pathom3.connect.runner :as pcr]
    ;[com.wsscode.pathom3.error :as p.error]
    ;[com.wsscode.pathom3.format.eql :as pf.eql]
    ;[com.wsscode.pathom3.path :as p.path]
   [ixn.schema.account :refer [fetch-accounts fetch-account-by-id pull-account-by-id]]
   [ixn.schema.transaction :refer [pull-transaction-by-account]]))
; pull stored account information

(pco/defresolver
  transactions
  [{:keys [:transaction/account]}]
  {::pco/output
   [:transaction/id
    :transaction/line
    :transaction/account
    :transaction/description
    :transaction/journal
    :transaction/year
    :transaction/period
    :transaction/date
    :transaction/amount
    :transaction/sub-admin
    :transaction/cost-center
    :transaction/side
    :transaction/invoice]}
  (pull-transaction-by-account account))

(pco/defresolver
  accounts-by-id
  [{:keys [:account/id]}]
  {::pco/output
   [:account/type
    :account/name
    :account/summary-level
    :xt/id]}
  (pull-account-by-id id))

(def env
  (pci/register
   [accounts-by-id transactions]))

(comment
  (pull-account-by-id "80100")
  (fetch-account-by-id "80100")
  (fetch-accounts)
  (pull-transaction-by-account "80100")
  ;process needs an environment configurationx
  (p.eql/process
   env
    ; we can provide some initial data
   {:account/id "80100"}
    ; then we write an EQL query
   [:account/type :account/name :account/summary-level :xt/id])

  (p.eql/process
   env
   {:transaction/account "80100"}
   [:transaction/id :transaction/line :transaction/amount :transaction/side])
  ...)

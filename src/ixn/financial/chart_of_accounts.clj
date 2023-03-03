(ns ixn.financial.chart-of-accounts
  (:require
   [rum.core :as rum]
   [ixn.schema.account :refer [fetch-account-by-id pull-account-by-id pull-all-accounts]]))


(comment
  (pull-account-by-id "80100")
  (fetch-account-by-id "80100")
  (fetch-account-by-id "12010")
  (pull-all-accounts {:sort-on :account/name :order :asc :limit 10})
  (rum/render-html (form))
  (rum/render-html (overview))
  ())

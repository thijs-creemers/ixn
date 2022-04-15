(ns ixn.financial.chart-of-accounts
  (:require [xtdb.api :as xtdb]
            [ixn.db :refer [xtdb-node]]))

(defn- fetch-account
  "Fetch an account by id"
  [id]
  (let [res (xtdb/q
              (xtdb/db xtdb-node)
              '{:find     [?act ?id]
                :where    [[?act :account/id ?id]
                           [?act :account/summary-level 0]]
                :in       [?id]
                :order-by [[?id :asc]]}
              id)]
    (if (= 1 (count res))
      (ffirst res)
      (throw (str "Unknown account number " id)))))


(comment
  (fetch-account "80100")
  (fetch-account "12010"))

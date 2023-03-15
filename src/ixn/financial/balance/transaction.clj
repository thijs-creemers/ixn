(ns ixn.financial.balance.transaction
  (:require
    [ixn.state :refer [xtdb-node]]
    [xtdb.api :as xtdb]))
"
[?trn :transaction/id ?id]
[?trn :transaction/line ?line]
[?trn :transaction/account ?account]
[?trn :transaction/description ?description]
[?trn :transaction/journal ?journal]
[?trn :transaction/year ?year]
[?trn :transaction/period ?period]
[?trn :transaction/date ?date]
[?trn :transaction/amount ?amount]
[?trn :transaction/sub-admin ?sub-admin]
[?trn :transaction/cost-center ?cost-center]
[?trn :transaction/side ?side]
[?trn :transaction/invoice ?invoice]
"
(defn pull-all-transactions
  [{:keys [sort-on order limit offset]}]

  (let [my-limit  (if (and limit (> limit 0)) limit 10)
        my-offset (if offset offset 0)
        data      (->> (xtdb/q
                         (xtdb/db (xtdb-node))
                         '{:find     [(pull ?trn ?account ?year ?period[*])]
                           :where    [[?trn :transaction/id _]
                                      [?trn :transaction/account _]
                                      [?trn :transaction/year _]
                                      [?trn :transaction/period _]]
                           :order-by [[?account :asc] [?year :desc] [?period :desc]]})
                       (map first)
                       (sort #(compare (sort-on %1) (sort-on %2)))
                       (drop my-offset)
                       (take my-limit))]
    (if (= order :asc) data (reverse data))))

(defn fetch-all-transactions
  [{:keys [sort-on order year period]}]
  (let [;my-limit  (if (and limit (> limit 0)) limit 10)
        ;my-offset (if offset offset 0)
        data      (->> (xtdb/q
                         (xtdb/db (xtdb-node))
                         '{:find     [?id ?line ?account ?description ?journal ?year ?period ?date ?amount ?sub-admin ?cost-center ?side ?account-name]
                           :keys     [id line account description journal year period date amount sub-admin cost-center side account-name]
                           :in [year period]
                           :where    [[?trn :transaction/id ?id]
                                      [?trn :transaction/line ?line]
                                      [?trn :transaction/account ?account]
                                      [?act :account/id ?account]
                                      [?act :account/name ?account-name]
                                      [?trn :transaction/description ?description]
                                      [?trn :transaction/journal ?journal]
                                      [?trn :transaction/year ?year]
                                      [?trn :transaction/year year]
                                      [?trn :transaction/period ?period]
                                      [?trn :transaction/period period]
                                      [?trn :transaction/date ?date]
                                      [?trn :transaction/amount ?amount]
                                      [?trn :transaction/sub-admin ?sub-admin]
                                      [?trn :transaction/cost-center ?cost-center]
                                      [?trn :transaction/side ?side]]}
                         year period)
                       (sort #(compare (sort-on %1) (sort-on %2))))]
                       ;(drop my-offset)
                       ;(take my-limit))]
    (if (= order :asc) data (reverse data))))

(comment

  (-> {:sort-on :transaction/account :order :asc :limit 10000 :offset 0 :year 2023 :period 3}
      (fetch-all-transactions))

  ())
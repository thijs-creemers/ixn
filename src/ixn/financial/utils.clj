(ns ixn.financial.utils
  (:require [ixn.schema.money :refer [<-money]]))

(defn line-totals [s line]
  (let [{:keys [:transaction/amount :transaction/side]} line
        calc-amount (<-money amount)]
    (if (= :debit side)
      [(+ (first s) calc-amount) (last s)]
      [(first s) (+ (last s) calc-amount)])))


(defn balance [lines]
  (let [[d c] (reduce line-totals [0.0 0.0] lines)]
    (= 0.0 (- d c))))


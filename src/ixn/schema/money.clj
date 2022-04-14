(ns ixn.schema.money
  (:require [clojure.string :as str]
            [ixn.schema.currency :as ic]
            [malli.core :as m]))


;; Malli definition
(def Money
  [:map {:closed true :en "money" :nl "geld"}
   [:money/currency {:en "currency" :nl "valuta"}  (-> ic/currencies
                                                       keys
                                                       (conj :enum)
                                                       vec)]
   [:money/value {:en "value" :nl "waarde"} int?]])


;; Constants
(defonce default-currency :EUR)
(defonce decimal-separator ",")
(defonce thousand-separator ".")


(defn round
  [value decimals]
  "Round a double to the given precision (number of significant digits)"
  (let [factor (Math/pow 10 decimals)]
    (/ (Math/round ^Double (* value factor)) factor)))


(defn money?
  [val]
  "Check id val is valid Money data."
  (m/validate Money val))


(defn ->money
  ([currency-code value]
   (let [currency-rec (currency-code ic/currencies)
         decimals (:currency/decimals currency-rec)]
     {:money/currency currency-code
      :money/value    (long (* (round value decimals) (Math/pow 10 decimals)))}))
  ([value]
   (->money default-currency value)))

(defn <-money
  "Return a big decimal value."
  [money-value]
  (let [value (:money/value money-value)
        currency-rec ((get money-value :money/currency :EUR) ic/currencies)
        decimals (:currency/decimals currency-rec)]
    (bigdec (/ value (Math/pow 10 decimals)))))

(defn <-money-value
  [money-value]
  "Return integer value as registered in money data"
  (:money/value money-value))


(defn mformat
  "Format money into string with currency symbol and thousand separation."
  [money-value]
  (let [{:currency/keys [symbol decimals]} ((:money/currency money-value) ic/currencies)
        value (str (:money/value money-value))
        dec-part (subs value (- (count value) decimals))
        int-part (subs value 0 (- (count value) decimals))
        int-partseparated (->> int-part
                               reverse
                               (partition 3 3 "@")
                               (map (fn [x] (str/replace (str/join (reverse x)) #"@" "")))
                               reverse
                               (str/join thousand-separator))]
    (str symbol " " int-partseparated decimal-separator dec-part)))



(defn ->money-from-value
  "returns a money value"
  ([currency value]
   {:money/currency currency
    :money/value    value})
  ([value]
   (->money-from-value default-currency value)))



(defn add
  "Add"
  [& args]
  (->money-from-value (apply + (map (fn [x] (:money/value x)) args))))


(defn subtract
  "Subtract"
  [& args]
  (->money-from-value (apply - (map (fn [x] (:money/value x)) args))))


(comment
  ;; some REPL tests
  (money?
    (add (->money 12.3456) (->money 11.33383838)))
  (money? (subtract (->money 12.3456) (->money 11.33383838)))
  (->money 12.34))

(ns ixn.money
  (:require [malli.core :as m]
            [clojure.string :as str]
            [clojurewerkz.money.amounts :as cwma]
            [clojurewerkz.money.currencies :as cwmc]))

;; Malli definition
(def Currency
  [:map
   [:currency/code-2 {:en "ISO 2 code" :nl "ISO 2 code"}
    [:re {:error-message {:en "Invalid ISO-2 code." :nl "Ongeldige ISO-2 code."}} #"^[A-Z]{2}$"]]
   [:currency/code-3 {:en "ISO 3 code" :nl "ISO 2 code"}
    [:re {:error-message {:en "Invalid ISO-3 code." :nl "Ongeldige ISO-3 code."}} #"^[A-Z]{3}$"]]
   [:currency/symbol {:en "symbol" :nl "symbool"} string?]
   [:currency/decimals {:en "decimals" :nl "decimalen"} [:and int? [:>= 0] [:<= 5]]]])

(def Money
  [:map {:closed true :en "money" :nl "geld"}
   [:money/currency {:en "currency" :nl "valuta"} Currency]
   [:money/value {:en "value" :nl "waarde"} int?]])

(def Poen org.joda.money.Money)

;; A list of currencies
(def currencies
  {:eur {:currency/code-2 "EU" :currency/code-3 "EUR" :currency/symbol "€" :currency/decimals 2}
   :usd {:currency/code-2 "US" :currency/code-3 "USD" :currency/symbol "$" :currency/decimals 2}})

;; Constants
(defonce default-currency (:eur currencies))
(defonce decimal-separator ",")
(defonce thousand-separator ".")

(defn round
  "Round a double to the given precision (number of significant digits)"
  [precision d]
  (let [factor (Math/pow 10 precision)]
    (/ (Math/round ^Double (* d factor)) factor)))

(defn money? [val]
  "Check id val is valid Money data."
  (m/validate Money val))

(defn ->money
  ([currency value]
   (let [decimals (:currency/decimals currency)]
     {:money/currency currency
      :money/value (long (* (round decimals value) (Math/pow 10 decimals)))}))
  ([value]
   (->money default-currency value)))

(defn- ->money-from-value
  ([currency value]
   {:money/currency currency
     :money/value value})
  ([value]
   (->money-from-value default-currency value)))

(defn <-money
  "Return a big decimal value."
  [money-value]
  (let [value (:money/value money-value)
        decimals (get-in money-value [:money/currency :currency/decimals])]
    (bigdec (/ value (Math/pow 10 decimals)))))

(defn mformat
  "Format money into string with currency symbol and thousand separation."
  [money-value]
  (let [symbol (get-in money-value [:money/currency :currency/symbol])
        value (str (:money/value money-value))
        decimals (get-in money-value [:money/currency :currency/decimals])
        dec-part (subs value  (- (count value) decimals))
        int-part (subs value 0 (- (count value) decimals))
        int-partseparated (->> int-part
                               reverse
                               (partition 3 3 "@")
                               (map (fn [x] (str/replace (str/join (reverse x)) #"@" "")))
                               reverse
                               (str/join thousand-separator))]
    (str symbol " " int-partseparated decimal-separator dec-part)))


(defn add
  "Add"
  [& args]
  (->money-from-value (apply + (map (fn [x] (:money/value x)) args))))

(defn subtract
  "Subtract"
  [& args]
  (->money-from-value (apply - (map (fn [x] (:money/value x)) args))))

(defn multiply
  "multiply"
  [& args]
  [(apply * (map (fn [[v _]] v) args))
   (apply min (map (fn [[_ v]] v) args))])

(defn divide
  "divide"
  [& args]
  [(apply / (map (fn [[v _]] v) args))
   (apply min (map (fn [[_ v]] v) args))])

(comment
  ;; clojurewerkz money
  (cwma/amount-of cwmc/USD 10.5)
  (cwma/currency-of
    (cwma/parse "EUR 10.50"))
  (cwma/minor-of
    (cwma/parse "EUR 10.50"))
  (cwma/major-of
    (cwma/parse "EUR 10.50"))



  ;; some REPL tests
  (money? (plus (->money 12.3456) (->money 11.33383838)))
  (money? (minus (->money 12.3456) (->money 11.33383838)))
  (<-money (multiply (->money 12.3456) (->money 11.33383838) (->money 2.1)))
  (divide (->money 12.3456) 2)
  (apply min [1 2 3 4])
  (->money 12.345))

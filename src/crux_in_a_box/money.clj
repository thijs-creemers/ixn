(ns crux-in-a-box.money
  (:require [malli.core :as m]
            [malli.generator :as mg]))

(defn round
  "Round a double to the given precision (number of significant digits)"
  [precision d]
  (let [factor (Math/pow 10 precision)]
    (/ (Math/round (* d factor)) factor)))

(def Money
   [:cat int? [:int {:min 0 :max 5}] number?])

(comment
  (mg/generate Money))
;  ;; (m/encode Money (mg/generate Money) (mt/key-transformer {:encode name}))
;  ;; (m/encode Money (str {"v" -55000, "p" 4, "o" -5.5})  (mt/json-transformer)))

(defn money? [val]
 (m/validate Money val))

(defn ->money
  "a money value consists of a vector with 3 values `money represented as long int`, `precision int` and the `original value`."
  ([value precision]
   [(long (* (round precision value) (Math/pow 10 precision))) precision value])
  ([value]
   (->money value 2)))

(defn <-money
  "Return a big decimal value."
  [money-value]
  (let [[value precision _] money-value]
    (bigdec (/ value (Math/pow 10 precision)))))

(comment
  (money? 1234)
  (money? (->money 12.34))
  (<-money (->money 12.24655)))

(defn rounded
  ([value precision]
   (-> value
       (->money precision)
       <-money))
  ([value]
   (rounded value 2)))

(defn plus
  "Add"
  [& args]
  [(apply + (map (fn [[v _ _]] v) args))
   (apply min (map (fn [[_ v _]] v) args))
   (apply + (map (fn [[_ _ v]] v) args))])

(defn minus
  "minus"
  [& args]
  [(apply - (map (fn [[v _ _]] v) args))
   (apply min (map (fn [[_ v _]] v) args))
   (apply - (map (fn [[_ _ v]] v) args))])

(defn multiply
  "multiply"
  [& args]
  [(apply * (map (fn [[v _ _]] v) args))
   (apply min (map (fn [[_ v _]] v) args))
   (apply * (map (fn [[_ _ v]] v) args))])

(defn divide
  "divide"
  [& args]
  [(apply / (map (fn [[v _ _]] v) args))
   (apply min (map (fn [[_ v _]] v) args))
   (apply / (map (fn [[_ _ v]] v) args))])

(comment
  (money? (plus (->money 12.3456) (->money 11.33383838)))
  (money? (minus (->money 12.3456) (->money 11.33383838)))
  (<-money (multiply (->money 12.3456) (->money 11.33383838) (->money 2.1)))

  (rounded 12.344)
  (apply min [1 2 3 4])
  (->money 12.345))

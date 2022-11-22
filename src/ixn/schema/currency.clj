(ns ixn.schema.currency
  (:require [clj-http.client :as client]
            [jsonista.core :as json]))


(defonce rates-cache (atom {:date "1900-01-01" :rates []}))

(def Currency
  [:map
   [:currency/iso-code {:en "ISO 3 code" :nl "ISO 2 code"}
    [:re {:error-message {:en "Invalid ISO-3 code." :nl "Ongeldige ISO-3 code."}} #"^[A-Z]{3}$"]]
   [:currency/symbol {:en "symbol" :nl "symbool"} string?]
   [:currency/decimals {:en "decimals" :nl "decimalen"} [:and int? [:>= 0] [:<= 5]]]])


;; A list of currencies, add if needed.
(def currencies
  {:EUR {:currency/iso-code "EUR" :currency/iso-numeric "978" :currency/symbol "€"  :currency/decimals 2 :currency/name "Euro"}
   :USD {:currency/iso-code "USD" :currency/iso-numeric "840" :currency/symbol "$"  :currency/decimals 2 :currency/name "US Dollar"}
   :GBP {:currency/iso-code "GBP" :currency/iso-numeric "826" :currency/symbol "£"  :currency/decimals 2 :currency/name "British Pound"}
   :CHF {:currency/iso-code "CHF" :currency/iso-numeric "756" :currency/symbol "Fr" :currency/decimals 2 :currency/name "Swiss franc"}
   :AUD {:currency/iso-code "AUD" :currency/iso-numeric "036" :currency/symbol "$"  :currency/decimals 2 :currency/name "Australian dollar"}
   :JPY {:currency/iso-code "JPY" :currency/iso-numeric "392" :currency/symbol "¥"  :currency/decimals 2 :currency/name "Japanese Yen"}
   :SEK {:currency/iso-code "SEK" :currency/iso-numeric "752" :currency/symbol "kr" :currency/decimals 2 :currency/name "Swedish krona"}
   :DKK {:currency/iso-code "DKK" :currency/iso-numeric "208" :currency/symbol "kr" :currency/decimals 2 :currency/name "Danish krona"}
   :NOK {:currency/iso-code "NOK" :currency/iso-numeric "578" :currency/symbol "kr" :currency/decimals 2 :currency/name "Norwegian krona"}
   :CAD {:currency/iso-code "CAD" :currency/iso-numeric "124" :currency/symbol "$"  :currency/decimals 2 :currency/name "Canadian dollar"}
   :PLN {:currency/iso-code "PLN" :currency/iso-numeric "985" :currency/symbol "zł",:currency/decimals 2 :currency/name "Polish Złoty"}})


(defn date-yesterday []
  (.format (java.text.SimpleDateFormat. "yyyy-MM-dd") (java.util.Date. ^Long (- (.getTime (java.util.Date.)) (* 24 60 60 1000)))))

(defn- fetch-rates
  []
  (when (< (compare (:date @rates-cache) (date-yesterday)) 0)
     (let [{:keys [reason status :body]} (client/get "https://www.currency-api.com/rates")]
       (if (= reason "OK")
         (reset! rates-cache (-> body (json/read-value json/keyword-keys-object-mapper)))
         {:error {:status status :reason reason}})))
  (:rates @rates-cache))


(defn rate
  "Base rate is EUR"
  [currency]
  (currency (fetch-rates)))

(defn convert-to-euro [currency amount]
  (let [rate (rate currency)]
    (/ (bigdec amount) rate)))

;(defn convert [to-currency currency amount]
;  (let [rate (rate currency)
;        euros (convert-to-euro currency amount)]
;    (prn euros)
;    (/ (bigdec euros) rate)))

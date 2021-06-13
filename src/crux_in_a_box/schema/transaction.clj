(ns crux-in-a-box.schema.transaction
  (:require
   [crux-in-a-box.schema.account :refer [AccountNumber]]
   [crux-in-a-box.schema.core :refer [NotEmptyString]]
   [malli.generator :as mg]
   [malli.core :as m]
   [malli.error :as me]))


(def TransactionLine
  [:map
   {:closed true
    :title {:en "Transaction line" :nl "Transactie regel"}}
   [:transaction/id uuid?]
   [:transaction/line-number pos-int?]
   [:transaction/account-number AccountNumber]
   [:transaction/description NotEmptyString]
   [:transaction/date inst?]
   [:transaction/amount number?]
   [:transaction/sub-admin string?]
   [:transaction/cost-center string?]
   [:transaction/side [:enum {} :debit :credit]]])

(defn- line-totals [s line]
  (let [{:keys [:transaction/amount :transaction/side]} line]
    (if (= :debit side)
      [(+ (first s) amount) (last s)]
      [(first s) (+ (last s) amount)])))

(defn- balance [lines]
  (let [[d c] (reduce line-totals [0.0 0.0] lines)]
    (= 0.0 (- d c))))

(def Transaction
  [:and
   [:vector TransactionLine]])
   ;[:= 0.0 [:fn (balance [:vector TransactionLine])]]])

(defn uuid []
  (java.util.UUID/randomUUID))
(defn date []
  (java.util.Date.))

(def transaction-check
  (let [id (uuid)
        t-date (date)]
    [{:transaction/id id
      :transaction/line-number 1
      :transaction/date t-date
      :transaction/account-number "12345"
      :transaction/description "bb"
      :transaction/cost-center ""
      :transaction/sub-admin ""
      :transaction/amount 10.0
      :transaction/side :credit}
     {:transaction/id id
      :transaction/line-number 2
      :transaction/date t-date
      :transaction/account-number "22334"
      :transaction/description "bb"
      :transaction/cost-center ""
      :transaction/sub-admin ""
      :transaction/amount 5.0
      :transaction/side :debit}
     {:transaction/id id
      :transaction/line-number 3
      :transaction/date t-date
      :transaction/account-number "22442"
      :transaction/description "bb"
      :transaction/cost-center ""
      :transaction/sub-admin ""
      :transaction/amount 5.0
      :transaction/side :debit}]))


(comment
 (m/explain Transaction transaction-check)
 (mg/generate TransactionLine)
 (line-totals [5.0 0.0] {:transaction/amount 10.0
                         :transaction/side :credit})
 (balance transaction-check)
 transaction-check)

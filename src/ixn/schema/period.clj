(ns ixn.schema.period
  (:require [clj-time.core :as tc]))


(def BookYear
  [:map
   {:closed? true}
   [:book-year/year pos-int?]
   [:book-year/start-date inst?]
   [:book-year/end-date inst?]])


(def BookPeriod 
  [:map
   {:closed true}
   [:book-period/year BookYear]
   [:book-period/number pos-int?]
   [:book-period/active boolean?]
   [:book-period/start-date inst?]
   [:book-period/end-date inst?]])


(defn create-book-year [year]
  (let
    [periods (range 1 13)
     result-list (mapv
                   (fn [period]
                     {:book-period/year year
                      :book-period/number period
                      :book-period/active false
                      :book-period/start-date (tc/first-day-of-the-month year period)
                      :book-period/end-date (tc/last-day-of-the-month year period)})
                   periods)]
    (conj result-list {:book-year/year year
                       :book-year/start-date (tc/first-day-of-the-month year 1)
                       :book-year/end-date (tc/last-day-of-the-month year 12)})))



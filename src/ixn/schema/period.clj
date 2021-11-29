(ns ixn.schema.period)

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
   [:book-period/end-date-date inst?]])
(ns ixn.utils
  (:require [jsonista.core :as json]
            [ixn.settings :refer [date-format]]))

(defn uuid []
  (java.util.UUID/randomUUID))

(defn now []
  (java.util.Date.))

(defn parse-integer [number-string, radix]
  (try (Integer/parseInt number-string radix)
       (catch Exception _ nil)))

(defn ->json [edn-value]
  (-> edn-value
      (json/write-value-as-bytes json/default-object-mapper)
      (json/read-value json/default-object-mapper)))

(defn format-date [date]
  (.format (java.text.SimpleDateFormat. date-format) date))
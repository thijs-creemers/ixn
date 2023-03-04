(ns ixn.utils
  (:require [jsonista.core :as json]))

(defn uuid []
  (java.util.UUID/randomUUID))

(defn now []
  (java.util.Date.))

(defn parse-integer [number-string, radix]
  (try (Integer/parseInt number-string radix)
       (catch Exception e nil)))

(defn ->json [edn-value]
  (-> edn-value
      (json/write-value-as-bytes json/default-object-mapper)
      (json/read-value json/default-object-mapper)))

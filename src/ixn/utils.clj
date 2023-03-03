(ns ixn.utils)

(defn uuid []
  (java.util.UUID/randomUUID))

(defn now []
  (java.util.Date.))

(defn- parse-integer [number-string, radix]
  (try (Integer/parseInt number-string radix)
       (catch Exception e nil)))

(ns ixn.utils)

(defn uuid []
  (java.util.UUID/randomUUID))

(defn now []
  (java.util.Date.))
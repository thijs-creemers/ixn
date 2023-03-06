(ns ixn.state)

(defonce system (atom nil))

(defn xtdb-node []
  (:database (:database @system)))

(defn http-server []
  (:web/server (:web/server @system)))

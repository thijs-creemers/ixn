(ns ixn.schema.core
  (:require [clojure.string :as string]))

(def NotEmptyString [:and
                     string?
                     [:fn {:error/message "Value is required."}
                      #(seq %)]])

(def UppercaseString [:and
                      string?
                      [:fn {:error/message "String must be in uppercase characters."}
                       #(= % (string/upper-case %))]])

(defn ff-uitproberen []
  (println "Hallo")
  (+ (* 2 3)
     (+ 2 4))

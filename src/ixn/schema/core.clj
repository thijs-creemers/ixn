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

(def LowercaseString [:and
                      string?
                      [:fn {:error/message "String must be in lowercase characters."}
                       #(= % (string/lower-case %))]])

(ns crux-in-a-box.schema.ipam
  (:require
   [clojure.network.ip :as ip]
   ;; [crux-in-a-box.schema.core :refer [NotEmptyString]]
   [malli.core :as m]
   ;; [malli.error :as me]
   [malli.generator :as mg])
   ;; [malli.provider :as mp]
  (:import (clojure.network.ip IPAddress)
           (javax.swing JFormattedTextField$AbstractFormatterFactory)))

;; Some IP validation functions.
(defn ip4-address?
  "Checks that string `value` is a valid IP 4 address."
  [value]
  (let [ip-version (try
                     (ip/version (ip/make-ip-address value))
                     (catch clojure.lang.ExceptionInfo _))]
    (= ip-version 4)))

(defn ip6-address?
  "Checks that string `value` is a valid IP 6 address."
  [value]
  (let [ip-version (try
                     (ip/version (ip/make-ip-address value))
                     (catch clojure.lang.ExceptionInfo _))]
    (= ip-version 6)))

(defn ip-address?
  "Checks that string `value` is a valid IP address this can be eiter an IP4 or IP6 address."
  [value]
  (let [ip-version (try
                     (ip/version (ip/make-ip-address value))
                     (catch clojure.lang.ExceptionInfo _))]
    (contains? #{4 6} ip-version)))

(defn ip-network?
  [value]
  (let [v (try
            (ip/make-network value)
            (catch clojure.lang.ExceptionInfo _))]
    (not= (count v) 1)))

(defn netmask?
  "Check if the string `value` is a valid netmask."
  [value]
  (let [num "(255|254|252|248|240|224|192|128|0+)"
        my-re (re-pattern (str "^" num "\\." num "\\." num "\\." num "$"))]
    (boolean (re-seq my-re value))))

(def cidr? ip-address?)

(def State [:enum :available :allocated :expired :reserved :suspended :allocated-subnet])

(declare Address)

(def IP4Address
  [:re #"(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)(\.(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)){3}"])

(def IP6Address
  [:re #"(([0-9a-fA-F]{1,4}:){7,7}[0-9a-fA-F]{1,4}|([0-9a-fA-F]{1,4}:){1,7}:|([0-9a-fA-F]{1,4}:){1,6}:[0-9a-fA-F]{1,4}|([0-9a-fA-F]{1,4}:){1,5}(:[0-9a-fA-F]{1,4}){1,2}|([0-9a-fA-F]{1,4}:){1,4}(:[0-9a-fA-F]{1,4}){1,3}|([0-9a-fA-F]{1,4}:){1,3}(:[0-9a-fA-F]{1,4}){1,4}|([0-9a-fA-F]{1,4}:){1,2}(:[0-9a-fA-F]{1,4}){1,5}|[0-9a-fA-F]{1,4}:((:[0-9a-fA-F]{1,4}){1,6})|:((:[0-9a-fA-F]{1,4}){1,7}|:)|fe80:(:[0-9a-fA-F]{0,4}){0,4}%[0-9a-zA-Z]{1,}|::(ffff(:0{1,4}){0,1}:){0,1}((25[0-5]|(2[0-4]|1{0,1}[0-9]){0,1}[0-9])\.){3,3}(25[0-5]|(2[0-4]|1{0,1}[0-9]){0,1}[0-9])|([0-9a-fA-F]{1,4}:){1,4}:((25[0-5]|(2[0-4]|1{0,1}[0-9]){0,1}[0-9])\.){3,3}(25[0-5]|(2[0-4]|1{0,1}[0-9]){0,1}[0-9]))"])

(def IP [:or IP4Address IP6Address])

(comment
  (ip/make-ip-address "1.2.3.5")
  (ip-address? "145.1.1.12")
  (ip-network? "145.1.1.0/24")
  (m/validate IP "2001::1")
  (m/validate IP4Address "192.168.1.1")
  (m/validate IP6Address "2001::168:1:1")
  (mg/generate Address))

(def Prefix
  [:map
   {:title {:en "Prefix / Subnet" :nl "Prefix / Subnet"}}
   [:prefix/id pos-int?]
   [:prefix/state State]
   [:prefix/tags [:set [:re #"^[A-Z]{3,8}$"]]]
   ;[:prefix/prefix ip-network?]
   [:prefix/parent Prefix]
   [:prefix/description string?]
   [:prefix/index pos-int?]
   [:prefix/version [:enum {:title {:en "ip version" :nl "ip versie"}} 1 6]]
   [:prefix/subnet? boolean?]
   ;[:prefix/addresses [:set Address]]
   [:prefix/subnets [:set Prefix]]])

(def Address
  [:map
   {:address/title {:en "Ip Address" :nl "IP Adres"}}
   [:address/name string?]
   [:address/address IP]
   [:address/address-id pos-int?]
   [:address/description string?]
   [:address/prefix Prefix]
   [:address/state State]
   [:address/tags [:set keyword?]]
   [:address/vrf pos-int?]])

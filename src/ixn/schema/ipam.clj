(ns ixn.schema.ipam
  (:require
   [clojure.network.ip :as ip]
   ;; [ixn.schema.core :refer [NotEmptyString]]
   [malli.core :as m]
   ;; [malli.error :as me]
   [malli.generator :as mg]))
  ;;  [malli.provider :as mp])


;; Make sure Address and Network are declared and the clj single pass compile works
(declare Address)
(declare Network)


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

(def State [:enum :available :allocated :expired :reserved :suspended :allocated-network])

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

(def Network
  [:map
   {:ip-net/title {:en "Prefix / Network" :nl "Prefix / Network"}}
   [:ip-net/id pos-int?]
   [:ip-net/state State]
   [:ip-net/terminal? boolean?]
   [:ip-net/tags [:set [:re #"^[A-Z]{3,8}$"]]]
   [:ip-net/parent Network]
   [:ip-net/description string?]
   [:ip-net/index pos-int?]
   [:ip-net/version [:enum {:title {:en "ip version" :nl "ip versie"}} 1 6]]
   [:ip-net/network? boolean?]
   [:ip-net/addresses [:set Address]]
   [:ip-net/networks [:set Network]]])

(def Address
  [:map
   {:ip-add/title {:en "Ip Address" :nl "IP Adres"}}
   [:ip-add/name string?]
   [:ip-add/address IP]
   [:ip-add/address-id pos-int?]
   [:ip-add/description string?]
   [:ip-add/network Network]
   [:ip-add/state State]
   [:ip-add/tags [:set keyword?]]
   [:ip-add/vrf pos-int?]])


(comment
  (:ip-add/network (mg/generate Address)))
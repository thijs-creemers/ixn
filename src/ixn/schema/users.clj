(ns ixn.schema.users
  (:require
    [buddy.hashers :as hashers]
    [clj-otp.base32 :refer [decode-data]]
    [clj-otp.core :refer [hotp secret-key totp]]
    [clojure.string :as str]
    [clojure.tools.logging :as log]
    [ixn.db :refer [transact! xtdb-node]]
    [ixn.schema.core :refer [NotEmptyString]]
    [ixn.utils :refer [uuid]]
    [malli.core :as m]
    [malli.generator :as mg]
    [xtdb.api :as xtdb])

  (:import (java.security SecureRandom)))

(defn random-128-bit-string []
  (let [random-bytes (byte-array 16)]
    (.nextBytes (SecureRandom.) random-bytes)
    (apply str (map #(format "%02x" %) random-bytes))))

(def UserType [:enum {:title "Two factor auth type"} :totp :hotp])

(def User
  [:map
   {:closed? true :title {:en "user" :nl "gebruiker"}}
   [:user/id {:title {:en "account number" :nl "rekeningnummer"}} uuid?]
   [:user/user-name {:title {:en "username" :nl "gebruikersnaam"}} NotEmptyString]
   [:user/first-name {:title {:en "username" :nl "gebruikersnaam"}} string?]
   [:user/last-name {:title {:en "username" :nl "gebruikersnaam"}} string?]
   [:user/e-mail {:title {:en "e-mail" :nl "e-mail"}} NotEmptyString]
   [:user/active? {:title {:en "active" :nl "actief"}} boolean?]
   [:user/staff? {:title {:en "staff status" :nl "medewerker"}} boolean?]
   [:user/secret {:title {:en "password-salt" :nl "password-salt"}} string?]
   [:user/password {:title {:en "password" :nl "wachtwoord"}} string?]
   [:user/type] {:title {:en "two factor auth type" :nl "2 factor auth type"}} UserType])

(defn pull-user-by-user-name
  "Fetch a user record by his/her user name."
  [name]
  (ffirst (xtdb/q
            (xtdb/db xtdb-node)
            '{:find  [(pull ?user [*])]
              :in    [?name]
              :where [[?user :user/user-name ?name]]}
            name)))

(defn pull-user-by-e-mail
  "Fetch a user record by his/her e-mail address."
  [e-mail]
  (ffirst (xtdb/q
            (xtdb/db xtdb-node)
            '{:find  [(pull ?user [*])]
              :in    [?e-mail]
              :where [[?user :user/e-mail ?e-mail]]}
            e-mail)))

(defn all-users []
  (xtdb/q (xtdb/db xtdb-node)
          '{:find  [?act ?name ?e-mail]
            :where [[?act :user/user-name ?name]
                    [?act :user/e-mail ?e-mail]]}))

(defn email? [email]
  (if (re-matches #".+@.+\..+" email)
    true
    false))

(defn get-user
  "Fetch a user by name or e-mail"
  [name]
  (if (email? name)
    (pull-user-by-e-mail name)
    (pull-user-by-user-name name)))


(defn create-user [first-name last-name e-mail password]
  (prn e-mail)
  (let [password-salt (subs (random-128-bit-string) 0 16)
        new-user      {:user/id         (uuid)
                       :user/user-name  (str/lower-case (str first-name "." last-name))
                       :user/first-name (str/capitalize first-name)
                       :user/last-name  (str/capitalize last-name)
                       :user/e-mail     e-mail
                       :user/active?    true
                       :user/staff?     false
                       :user/type       :totp
                       :user/secret     password-salt
                       :user/password   (hashers/encrypt password {:salt password-salt})}
        valid?        (m/validate User new-user)]
    {:status valid?
     :value  (if valid?
               new-user
               (let [reason (m/explain User new-user)]
                 (log/error (str "user/create-user: "))
                 (:errors reason)))}))

(defn transact-user!
  "Create and store a new user"
  [first-name last-name e-mail password]
  (let [user-created (create-user first-name last-name e-mail password)]
    (if (:status user-created)
      (transact! [(:value user-created)])
      user-created)))

(defn- verify-password
  "Verify the user's password."
  [password user]
  (hashers/check-password password (:password user)))

(defn- get-secret
  "Get the user secret/salt by username"
  [user-name]
  (:usr/secret (pull-user-by-user-name user-name)))


(defn- verify-2fa
  "Verify the user's 2FA token."
  [token user]
  (let [secret     (get-secret (:username user))
        totp-value (if (= (:type user) :hotp)
                     (hotp secret (:counter user))
                     (totp secret (:interval user)))
        actual     (Integer/parseInt token)]
    (when (not (= totp-value actual))
      (throw (Exception. "Invalid 2FA token.")))))

(defn authenticate
  "Authenticate the user with the given username, password, and 2FA token."
  [username password token]
  (let [user (get-user username)]
    (when (and user
               (verify-password password user)
               (verify-2fa token user))
      user)))

(comment
  (transact-user! "Thijs" "Creemers" "thijs.creemers@gmail.com" "1@3hallo")
  (:value (create-user "Thijs" "Creemers" "thijs.creemers@gmail.com" "1@3hallo"))
  (all-users)
  (pull-user-by-e-mail "thijs.creemers@gmail.com")
  (mg/generate User)
  ;(hashers/encrypt "geheimpie" {:salt salt})
  (totp (decode-data "MFRGGZDFMZTWQ2LK"))
  (totp (decode-data (secret-key)))
  (hotp (decode-data "MFRGGZDFMZTWQ2LK") 2)
  (hashers/encrypt "geheimpje")
  (hashers/verify " geheimpie" "pbkdf2+sha512$209ce8498e0e6c15ae3bb8f3$100000$f7bde39b441dfa82e7192226f1ca276746d9c7acbafa5b92a623eabfd034172f20e630a87e166532b81fc2c0f921f02525836c414189b0252e2f74c3e9124de1")
  ...)

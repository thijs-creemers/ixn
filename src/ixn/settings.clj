(ns ixn.settings)

(defonce subadmin-accounts
         {:account-vat-high "27030"
          :vat-high         21
          :account-vat-low  "27040"
          :vat-low          9})

(defonce date-format "dd-MM-yyyy")
(defonce fetch-limit 20)
(def current-book-period {:year 2023 :period 3})


(def content-security-policy
  "default-src 'self'; script-src 'self' 'unsafe-inline'; style-src 'self' unpkg.com 'unsafe-inline';")
(ns crux-in-a-box.schema.relation
  (:require [crux-in-a-box.schema.core :refer [NotEmptyString UppercaseString]]
            [clojure.tools.logging :as log]
            [malli.core :as m]
            [malli.error :as me]
            [malli.generator :as mg]
            [clojure.tools.reader.edn :as edn]))

(def Country [:map {:closed? true?
                    :title   {:en "country" :nl "land"}}
              [:country/id {:title {:en "id" :nl "id"}} uuid?]
              [:country/iso-2 {:title {:en "ISO 2 code" :nl "ISO 2 code"}}
               [:re {:error-message {:en "Invalid ISO-2 code." :nl "Ongeldige ISO-2 code."}} #"^[A-Z]{2}$"]]
              [:country/iso-3 {:title {:en "ISO 3 code" :nl "ISO 3 code"}}
               [:re {:error-message {:en "Invalid ISO-3 code." :nl "Ongeldige ISO-2 code."}} #"^[A-Z]{3}$"]]
              [:country/name {:title {:en "name" :nl "naam"}} NotEmptyString]
              [:country/phone-prefix [:re
                                      {:error-message {:en "Invalid country prefix."
                                                       :nl "Ongeldig landnummer."}}
                                      #"^\+[0-9]{2,3}$"]]])
(comment
  (map (fn [_] (mg/generate Country)) (range 10)))

(def City [:map {:closed? true?
                 :title   {:en "city" :nl "plaats"}}
           [:city/id {:title {:en "id" :nl "id"}} uuid?]
           [:city/name {:title {:en "name" :nl "naam"}} NotEmptyString]
           [:city/phone-prefix [:re #"^0[1-9][0-9]{1,3}$"]]
           [:city/country Country]])

(comment
  (map (fn [_] (mg/generate City)) (range 10)))

(def AddressType [:enum {:title {:en "address type" :nl "soort adres"}} :home :office])

(def Latitude [:and double? [:> -90] [:< 90]])

(def Longitude [:and double? [:> -180] [:< 180]])

(def Address [:map {:closed? true :title {:en "address" :nl "adres"}}
              [:address/id {:title {:en "id" :nl "id"}} uuid?]
              [:address/street string?]
              [:address/number string?]
              [:address/city City]
              [:address/type AddressType]
              [:address/lat-lon [:map [:lat Latitude] [:lon Longitude]]]])

(def CommunicationPlatform [:enum
                            {:title {:en "communication type" :nl "communicatie type"}}
                            :email :phone :linkedin :facebook :instagram])

(comment
  (mg/generate CommunicationPlatform))
(def Communication [:map {:closed true :title {:en "Communication medium" :nl "Communicatie medium"}}
                    [:communication/id {:title {:en "id" :nl "id"}} uuid?]
                    [:communication/platform CommunicationPlatform]
                    [:communication/address string?]])      ; need some validation per type

(def Gender [:enum {:title {:en "gender" :nl "geslacht"}} :male :female :unknown])

(def Relation [:map {:title {:en "relation" :nl "relatie"}}
               [:relation/id {:title {:en "id" :nl "id"}} uuid?]
               [:relation/name {:title {:en "name" :nl "naam"}} string?]
               [:relation/first-name {:title {:en "first name" :nl "voornaam"}} string?]
               [:relation/last-name {:title {:en "last name" :nl "achternaam"}} string?]
               [:relation/initials {:title {:en "initials" :nl "initialen"}} string?]
               [:relation/gender {:title {:en "gender" :nl "geslacht"}} Gender]
               [:relation/date-of-birth {:title {:en "date of birth" :nl "geboortedatum"}} inst?]
               [:relation/date-of-incorporation {:title {:en "date of incorporation" :nl "datum oprichting"}} inst?]
               [:relation/registration_id string?]
               [:relation/vat-id string?]                   ;; can be validated externally
               [:relation/tags [:set keyword?]]
               [:relation/addresses [:repeat {:min 1 :max 5} Address]]
               [:relation/communication [:repeat {:min 1 :max 5} Communication]]])


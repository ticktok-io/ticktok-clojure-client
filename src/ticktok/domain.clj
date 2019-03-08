(ns ticktok.domain
  (:require [clojure.string :as string]
            [clojure.spec.alpha :as s]))

(def schedule-regex #"every.[1-9].\w+")

(s/def ::schedule-type
  (s/and string? #(re-matches schedule-regex %)))

(s/def ::name string?)

(s/def ::schedule ::schedule-type)

(s/def ::id string?)

(s/def ::queue string?)

(s/def ::uri string?)

(s/def ::channel (s/keys :req [::queue ::uri]))

(s/def ::clock-request (s/keys :req [::name ::schedule]))

(s/def ::url string?)

(s/def ::clock (s/keys :req [::channel ::id ::name ::schedule ::url]))

(s/def ::host string?)

(s/def ::token string?)

(s/def ::config (s/keys :req [::host ::token]))

(s/valid? ::clock-request {})

(s/explain ::clock-request {})

(defn valid-config? [config]
  (is-valid? ::config config))

(defn valid-clock-request? [clock-req]
  (is-valid? ::clock-request clock-req))

(defn is-valid? [type input]
  (let [parsed (s/conform type input)]
    (not= parsed ::s/invalid)))

(defn invalid-input [type input]
  (throw (ex-info "Invalid input" (s/explain-data type input))))

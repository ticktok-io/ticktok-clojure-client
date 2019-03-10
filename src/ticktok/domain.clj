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

(s/def ::clock-request (s/keys :req-un [::name ::schedule]))

(s/def ::url string?)

(s/def ::clock (s/keys :req-un [::channel ::id ::name ::schedule ::url]))

(s/def ::host string?)

(s/def ::token string?)

(s/def ::config (s/keys :req-un [::host ::token]))

(defn is-valid? [type input]
  (let [parsed (s/conform type input)]
    (not= parsed ::s/invalid)))

(defn conform-config [config]
  (s/conform ::config config))

(defn conform-clock-request [clock-req]
  (s/conform ::clock-request clock-req))

(defn valid-config? [config]
  (is-valid? ::config config))

(defn valid-clock-request? [clock-req]
  (is-valid? ::clock-request clock-req))

(defn invalid-input [type input]
  (println "----------- kdkdkd " type " ------ "  input)
  (throw (ex-info "Invalid input" (s/explain-data type input))))

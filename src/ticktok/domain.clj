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

(s/def ::channel (s/keys :req-un [::queue ::uri]))

(s/fdef ::callback
        :args any?
        :ret any?)

(s/def ::clock-request (s/keys :req-un [::name ::schedule]
                               :op-un [::callback]))

(s/def ::url string?)

(s/def ::clock (s/keys :req-un [::channel ::id ::name ::schedule ::url]))

(s/def ::host string?)

(s/def ::token string?)

(s/def ::config (s/keys :req-un [::host ::token]))

(defn conform-clock [clock]
  (s/conform ::clock clock))

(defn conform-config [config]
  (s/conform ::config config))

(defn conform-clock-request [clock-req]
  (s/conform ::clock-request clock-req))

(defn invalid-input [type input]
  (throw (ex-info "Invalid input" (s/explain-data type input))))

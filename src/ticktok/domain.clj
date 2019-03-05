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

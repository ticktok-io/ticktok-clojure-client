(ns ticktok.domain
  (:require [clojure.string :as string]
            [clojure.spec.alpha :as s]
            [clojure.data.json :as json]
            [ticktok.utils :refer [fail-with]]))

(s/def ::schedule-type string?)

(s/def ::name string?)

(s/def ::schedule ::schedule-type)

(s/def ::id string?)

(s/def ::queue string?)

(s/def ::uri string?)

(s/def ::url string?)

(s/def ::details (s/keys :req-un [(or [::queue ::url] [::url])]))

(s/def ::type (s/and string? #(contains? #{"rabbit" "http"} %)))

(s/def ::channel (s/keys :req-un [::details ::type]))

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

(defn valid? [type entity]
  (s/valid? type entity))

(defn conform [type entity]
  (s/conform type entity))

(defn conform-clock [clock]
  (conform ::clock clock))

(defn conform-clock-request [clock-req]
  (conform ::clock-request clock-req))

(defn validate-input [type entity]
  (let [parsed (conform type entity)]
    (if (= ::s/invalid parsed)
      (fail-with "Invalid input" (s/explain-data type entity))
      parsed)))

(defn parse-clock [raw]
  (let [cl-map (json/read-str raw :key-fn keyword)
        clock (conform ::clock cl-map)]
    (if (= ::s/invalid clock)
      (fail-with  "Failed to parse clock" {:clock raw})
      clock)))

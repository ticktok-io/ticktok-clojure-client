(ns ticktok.core
  (:require [org.httpkit.client :as http]
           [clojure.data.json :as json]
           [clojure.string :as string]
           [clojure.spec.alpha :as s]))

(def api "/api/v1/clocks")

(def schedule-regex #"every.[1-9].\w+")

(s/def ::schedule-type
  (s/and string? #(re-matches schedule-regex %)))

(s/def ::name string?)

(s/def ::schedule ::schedule-type)

(s/def ::clock-type (s/keys :req [::name ::schedule]))

(def clock {::name "myclock"
            ::schedule "every.5.seconds"})

(def bad-clock {::name "myclock"
                ::schedule "every.0.seconds"})

(s/valid? ::clock-type
          clock)

(s/explain ::clock-type
           bad-clock)

(s/conform ::clock-type
           clock)


(defn ticktok [host clock]
  (let [options {:headers  {"Content-Type" "application/json"}
                 :body (json/write-str clock)}
        endpoint (string/join [host api])
       {:keys [status body error]} @(http/post endpoint
                       options)]
    (= status 201)))

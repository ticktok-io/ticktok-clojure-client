(ns ticktok.core
  (require [org.httpkit.client :as http]
           [clojure.data.json :as json]
           [clojure.string :as string]
           [clojure.spec.alpha :as s]))

(def api "/api/v1/clocks")

(def schedule-regex #"[a-zA-Z]+.[1-9][0-9]*+.[a-zA-Z]+")

(s/def ::schedule-type (s/and string? #(re-matches schedule-regex %)))

(s/def ::name string?)

(s/def ::schedule ::schedule-type)

(s/def clock-type
  (s/keys :req [::name ::schedule]))

(defn ticktok [host clock]
  (let [options {:headers  {"Content-Type" "application/json"}
                 :body (json/write-str clock)}
        endpoint (string/join [host api])
       {:keys [status body error]} @(http/post endpoint
                       options)]
    (= status 201)))

(ns ticktok.fetcher
  (:require [org.httpkit.client :as http]
            [clojure.data.json :as json]
            [clojure.string :as string]
            [ticktok.domain :as dom]
            [clojure.spec.alpha :as s]
            [ticktok.rabbit :as rabbit]
            [ticktok.utils :refer [fail-with]]))

(def api "/api/v1/clocks")

(defn parse-clock [raw]
  (let [cl-map (json/read-str raw :key-fn keyword)
        clock (dom/conform-clock cl-map)]
    (if (= ::s/invalid clock)
      (fail-with  "Failed to parse clock" {:clock raw})
      clock)))

(defn fetch-clock [host clock-req]
  (let [options {:headers  {"Content-Type" "application/json"}
                 :body (json/write-str (select-keys clock-req [:name :schedule]))}
        endpoint (string/join [host api])
        {:keys [status body error]} @(http/post endpoint
                                                options)]
    (if (not= status 201)
      (fail-with  "Failed to fetch clock" {:status status
                                           :request clock-req})
      (parse-clock body))))

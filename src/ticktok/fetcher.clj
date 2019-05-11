(ns ticktok.fetcher
  (:require [org.httpkit.client :as http]
            [clojure.data.json :as json]
            [clojure.string :as string]
            [ticktok.domain :as dom]
            [clojure.spec.alpha :as s]
            [ticktok.rabbit :as rabbit]
            [ticktok.utils :refer [fail-with retry]]))

(def api "/api/v1/clocks")

(def default-attempts 6)

(defn- parse-clock [raw]
  (let [cl-map (json/read-str raw :key-fn keyword)
        clock (dom/conform ::dom/clock cl-map)]
    (println "raw " cl-map)
    (println "clock is " clock)
    (if (= ::s/invalid clock)
      (fail-with  "Failed to parse clock" {:clock raw})
      clock)))

(defn- fetch [{:keys [host token]} {:keys [name schedule] :as clock-req}]
  (let [options {:headers  {"Content-Type" "application/json"}
                 :query-params {:access_token token}
                 :body (json/write-str {:name name
                                         :schedule schedule})}
         endpoint (string/join [host api])
         {:keys [status body error]} @(http/post endpoint
                                                 options)]
    (println "ticktok: " body status error)
    (if (not= status 201)
       (fail-with  "Failed to fetch clock" {:status status
                                            :request clock-req})
       body)))

(defn fetch-clock
  ([config clock-req]
   (fetch-clock config clock-req default-attempts))
  ([config clock-req attempts]
   (let [clock (retry (fetch config clock-req) attempts)
         clock (parse-clock clock)]
     clock)))

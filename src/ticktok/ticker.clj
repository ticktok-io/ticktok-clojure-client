(ns ticktok.ticker
  (:require [org.httpkit.client :as http]
            [clojure.data.json :as json]
            [ticktok.domain :as dom]
            [clojure.string :as string]
            [ticktok.utils :refer [fail-with]]))

(def api "/api/v1/clocks")

(defn fetch-clock [{:keys [host token]} {:keys [name schedule]}]
  (let [url (string/join [host api])
        options {:headers  {"Content-Type" "application/json"}
                 :query-params {:name name
                                :schedule schedule
                                :access_token token}}
        {:keys [status body error]} @(http/get url
                                                options)]
    (println "ticktok repond with " status body)
    (dom/parse-clock body)))

(defn tick-on [{:keys [host token]} clock-id]
  (let [url (string/join [host api "/" clock-id "/tick"])
        options {:query-params {:access_token token}}
        {:keys [status body error]} @(http/put url
                                               options)]
    (if (not= status 204)
      (fail-with "Failed to tick for clock" {:clock-id clock-id
                                             :status status})
      true)))

(defn tick [config clock-request]
  (let [clock (fetch-clock config clock-request)]
    (tick-on config (:id clock))))

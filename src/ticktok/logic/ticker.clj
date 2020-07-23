(ns ticktok.logic.ticker
  (:require [org.httpkit.client :as http]
            [ticktok.domain :as dom]
            [ticktok.utils :refer [fail-with]]))

(def api "/api/v1/clocks")

(defn fetch-clock [{:keys [host token]} {:keys [name schedule]}]
  (let [url     (str host api)
        options {:query-params {:name         name
                                :schedule     schedule
                                :access_token token}
                 :as           :text}
        {:keys [status body _error]} @(http/get url
                                                options)]
    (if (not= status 200)
      (fail-with "Failed to fetch clock" {:clock  [name schedule]
                                          :status status})
      (dom/parse-clocks body))))

(defn tick-on [{:keys [host token]} {:keys [id] :as clock}]
  (let [url     (str host api "/" id "/tick")
        options {:query-params {:access_token token}}
        {:keys [status body error]} @(http/put url options)]
    (if (not= status 204)
      (fail-with "Failed to tick for clock" {:clock  clock
                                             :status status})
      true)))

(defn tick [config clock-request]
  (let [clock (fetch-clock config clock-request)]
    (tick-on config clock)))

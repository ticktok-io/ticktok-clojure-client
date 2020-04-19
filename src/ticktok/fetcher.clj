(ns ticktok.fetcher
  (:require [clojure.data.json :as json]
            [clj-http.client :as http]
            [clojure.string :as string]
            [ticktok.rabbit :as rabbit]
            [ticktok.domain :as dom]
            [ticktok.utils :refer [fail-with retry safe]]))

(def api "/api/v1/clocks")

(def default-attempts 6)

(defn- fetch [{:keys [host token]} {:keys [name schedule] :as clock-req}]
  (let [options {:query-params {:access_token token}
                 :body (json/write-str {:name name
                                        :schedule schedule})
                 :content-type :json}
        endpoint (string/join [host api])
        {:keys [status body]} (safe (http/post endpoint options))]
    (if (not= status 201)
      (fail-with  "Failed to fetch clock" {:status status
                                           :request clock-req})
      body)))

(defn fetch-clock
  ([config clock-req]
   (fetch-clock config clock-req default-attempts))
  ([config clock-req attempts]
   (let [clock (retry (fetch config clock-req) attempts)
         clock (dom/parse-clock clock)]
     clock)))

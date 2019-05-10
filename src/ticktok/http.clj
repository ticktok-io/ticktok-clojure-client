(ns ticktok.http
  (:require [ticktok.utils :refer [fail-with pretty]]
            [org.httpkit.client :as http]
            [clojure.data.json :as json]
            [overtone.at-at :as at]))

(def default-rate 1000)

(defonce state (atom {:pool nil
                      :tasks {}}))

(defn- pool []
  (:pool @state))

(defn- start! []
  (when (nil? (pool))
    (swap! state assoc :pool (at/mk-pool))))

(defn stop-task [t]
  (at/stop t))

(defn stop-tasks []
  (doseq [[c t] (:tasks @state)]
    (println "stopping " c)
    (stop-task (:task t))))

(defn shutdown-pool []
  (when-let [pool (pool)]
    (swap! state assoc :pool (do
                               (at/stop-and-reset-pool! pool :strategy :kill)
                               nil))))

(defn stop! []
  (stop-tasks)
  (shutdown-pool))

(defn- ticks [endpoint]
  (let [{:keys [status body error]} @(http/get endpoint {:as :text})
        parse #(json/read-str % :key-fn keyword)]
    (println endpoint "fetch: " status body)
    (if (= status 200)
      (parse body)
      nil)))

(defn invoke-on
  [url callback]
  (fn []
    (when (seq (ticks url))
      (callback))))

(defn make-task [url callback]
  (let [invoker (invoke-on url callback)
        t (at/every default-rate invoker (pool))]
    {:task t
     :url url}))

(defn- schedule-task [url clock callback]
  (swap! state update :tasks assoc clock (make-task url callback)))

(defn subscribe [url clock callback]
  (start!)
  (schedule-task url clock callback))

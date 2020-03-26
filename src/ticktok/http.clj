(ns ticktok.http
  (:require [ticktok.utils :refer [fail-with pretty]]
            [clj-http.client :as http]
            [clojure.data.json :as json]
            [overtone.at-at :as at]))

(def default-rate 1000)

(defonce state (atom {:pool nil
                      :tasks {}}))

(defn- pool []
  (:pool @state))

(defn- tasks []
  (:tasks @state))

(defn- init! []
  (when (nil? (pool))
    (swap! state assoc :pool (at/mk-pool))))

(defn- stop-task [t]
  (at/stop t))

(defn- stop-tasks []
  (doseq [[c t] (tasks)]
    (stop-task (:task t)))
  (swap! state assoc :tasks {}))

(defn- shutdown-pool []
  (when-let [pool (pool)]
    (swap! state update :pool #(do
                                 (at/stop-and-reset-pool! pool :strategy :kill)
                                 %))))
(defn stop! []
  (stop-tasks)
  (shutdown-pool))

(defn- ticks [clock-url]
  (let [{:keys [status body error]} (http/get clock-url {:as :clojure})]
    (if (= status 200)
      body
      nil)))

(defn- run-task [url callback]
  (let [callback-ref (atom callback)
        invoke-on-tick (fn []
                         (when (seq (ticks url))
                           (@callback-ref)))
        t (at/every default-rate invoke-on-tick (pool))]
    {:task t
     :url url
     :callback callback-ref}))

(defn- swap-callback! [{:keys [callback]} new-callback]
  (reset! callback  new-callback))

(defn- schedule-task [url clock callback]
  (if-let [t (get (tasks) clock)]
    (swap-callback! t callback)
    (swap! state update :tasks assoc clock (run-task url callback))))

(defn subscribe [url clock callback]
  (init!)
  (schedule-task url clock callback))

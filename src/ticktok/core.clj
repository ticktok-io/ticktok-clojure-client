(ns ticktok.core
  (:require [ticktok.domain :as dom]
            [ticktok.subscriber :as subscriber]
            [ticktok.fetcher :refer [fetch-clock]]
            [ticktok.rabbit :as rabbit]
            [ticktok.http :as http]
            [ticktok.utils :refer [fail-with pretty]]))

(def default-config {:host "http://localhost:8080"
                     :token "ticktok-zY3wpR"})

(declare ticktok)

(defn- dispatch-fn [config]
  (fn [command & args]
    (ticktok command config (first args))))

(defmulti ticktok (fn [op & _]
                    op))

(defmethod ticktok :start
  ([_]
   (ticktok :start default-config))
  ([_ config]
   (let [parsed-config (dom/validate-input ::dom/config config)]
     (dispatch-fn parsed-config))))

(defmethod ticktok :schedule
  ([_ clock-request]
   (ticktok :schedule default-config clock-request))
  ([_ config clock-request]
   (let [parsed-config (dom/validate-input ::dom/config config)
         parsed-request (dom/validate-input ::dom/clock-request clock-request)
         clock (fetch-clock parsed-config parsed-request)]
     (subscriber/subscribe-clock clock parsed-request)
     (dispatch-fn parsed-config))))

(defmethod ticktok :close [_]
  (rabbit/stop!)
  (http/stop!)
  nil)

(defmethod ticktok :default [op]
  (fail-with "Unknown command" {:command op}))

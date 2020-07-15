(ns ticktok.core
  (:require [ticktok.domain :as dom]
            [ticktok.subscriber :as subscriber]
            [ticktok.ticker :as ticker]
            [ticktok.fetcher :refer [fetch-clock]]
            [ticktok.rabbit :as rabbit]
            [ticktok.http :as http]
            [ticktok.utils :refer [fail-with pretty]]))

(def default-config {:host "http://localhost:9643"
                     :token "ticktok-zY3wpR"})

(declare ticktok)

(defn parse-input
  ([config]
   (dom/validate-input ::dom/config config)
   ([config clock-request]
    (let [parsed-config (parse-input config)
          parsed-request (dom/validate-input ::dom/clock-request clock-request)]
      [parsed-config, parsed-request]))))

(defn- dispatch-fn [config]
  (fn [command & args]
    (ticktok command config (first args))))

(defmulti ticktok (fn [op & _]
                    op))

(defmethod ticktok :start
  ([_]
   (ticktok :start default-config))
  ([_ config]
   (let [parsed-config (parse-input config)]
     (dispatch-fn parsed-config))))

(defmethod ticktok :schedule
  ([_ clock-request]
   (ticktok :schedule default-config clock-request))
  ([_ config clock-request]
   (let [[parsed-config, parsed-request] (parse-input config clock-request)
         clock (fetch-clock parsed-config parsed-request)]
     (subscriber/subscribe-clock clock parsed-request)
     (dispatch-fn parsed-config))))

(defmethod ticktok :tick
  ([_ clock-request]
   (ticktok :tick default-config clock-request))
  ([_ config clock-request]
   (let [[parsed-config, parsed-request] (parse-input config clock-request)]
     (ticker/tick parsed-config parsed-request)
     (dispatch-fn parsed-config))))

(defmethod ticktok :close [& _]
  (rabbit/stop!)
  (http/stop!)
  nil)

(defmethod ticktok :default [op]
  (fail-with "Unknown command" {:command op}))

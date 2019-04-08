(ns ticktok.core
  (:require [ticktok.domain :as dom]
            [ticktok.rabbit :as rabbit]
            [ticktok.fetcher :refer [fetch-clock]]
            [ticktok.utils :refer [fail-with pretty]]))

(def default-config {:host "http://localhost:8080"
                     :token "ticktok-zY3wpR"})

(defn- subscribe [{:keys [channel]} {:keys [callback]}]
  (rabbit/subscribe (:uri channel) (:queue channel) callback)
  nil)

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
     (subscribe clock parsed-request)
     (dispatch-fn parsed-config))))

(defmethod ticktok :stop [_]
  (rabbit/stop!)
  nil)

(defmethod ticktok :default [op] (fail-with "Unknown command" {:command op}))

(ns ticktok.core
  (:require [ticktok.domain :as dom]
            [ticktok.rabbit :as rabbit]
            [ticktok.fetcher :refer [fetch-clock]]
            [ticktok.utils :refer [fail-with pretty]]))

(defn- subscribe [{:keys [channel]} {:keys [callback]}]
  (rabbit/subscribe (:uri channel) (:queue channel) callback)
  nil)

(defn- make-clock [config]
  (fn [clock-request]
    (let [parsed-request (dom/validate-input ::dom/clock-request clock-request)
          clock (fetch-clock (:host config) parsed-request)]
      (subscribe clock parsed-request)
      true)))

(declare ticktok)

(defn dispatch-fn [config]
  (fn [command & args]
    (if (some? args)
      (ticktok command config (first args))
      (ticktok command config))))

(defmulti ticktok (fn [op & _]
                    op))

(defmethod ticktok :start [_ config]
  (let [parsed-config (dom/validate-input ::dom/config config)]
    (dispatch-fn parsed-config)))


(defmethod ticktok :stop [_]
  (rabbit/stop!)
  nil)

(defmethod ticktok :schedule [_ config clock-request]
  (let [parsed-config (dom/validate-input ::dom/config config)
        parsed-request (dom/validate-input ::dom/clock-request clock-request)
        clock (fetch-clock (:host parsed-config) parsed-request)]
    (subscribe clock parsed-request)
    (dispatch-fn parsed-config)))

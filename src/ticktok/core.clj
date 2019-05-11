(ns ticktok.core
  (:require [ticktok.domain :as dom]
            [ticktok.rabbit :as rabbit]
            [ticktok.http :as http]
            [ticktok.fetcher :refer [fetch-clock]]
            [ticktok.utils :refer [fail-with pretty]]))

(def default-config {:host "http://localhost:8080"
                     :token "ticktok-zY3wpR"})

(defmulti subscribe (fn [{:keys [channel]} _]
                       (keyword (:type channel))))

(defmethod subscribe :rabbit [{:keys [channel]} {:keys [callback]}]
  (let [details (:details channel)]
    (rabbit/subscribe (:uri details) (:queue details) callback))
  nil)

(defmethod subscribe :http [{:keys [channel id]} {:keys [callback]}]
  (let [url (get-in channel [:details :url])]
    (http/subscribe url id callback))
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

(defmethod ticktok :close [_]
  (rabbit/stop!)
  (http/stop!)
  nil)

(defmethod ticktok :default [op]
  (fail-with "Unknown command" {:command op}))

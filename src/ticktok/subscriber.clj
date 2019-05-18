(ns ticktok.subscriber
  (:require [ticktok.rabbit :as rabbit]
            [ticktok.http :as http]
            [ticktok.domain :as dom]))

(defonce clocks (atom {}))

(defprotocol Subscribble
  (subscribe [clock])
  (unsubscribe [clock]))

(deftype HttpClock [id url callback]
  Subscribble
  (subscribe [clock]
    (http/subscribe (.url clock) (.id clock) (.callback clock))))

(deftype RabbitClock [id uri queue callback]
  Subscribble
  (subscribe [clock]
    (rabbit/subscribe (.uri clock) (.queue clock) (.callback clock))))

(defmulti clock (fn [{:keys [channel id]} _]
                      (keyword (:type channel))))

(defmethod clock :rabbit [{:keys [channel id]} {:keys [callback]}]
  (let [details (:details channel)]
    (RabbitClock. id (:uri details) (:queue details) callback)))

(defmethod clock :http [{:keys [channel id]} {:keys [callback]}]
  (let [url (get-in channel [:details :url])]
    (HttpClock. id url callback)))

(defn subscribe-clock [clock clock-req]
  (subscribe (clock clock clock-req)))


(comment (defmulti subscribe (fn [{:keys [channel id]} _]
                               (keyword (:type channel))))

         (defmethod subscribe :rabbit [{:keys [channel]} {:keys [callback]}]
           (let [details (:details channel)]
             (rabbit/subscribe (:uri details) (:queue details) callback))
           nil)

         (defmethod subscribe :http [{:keys [channel id]} {:keys [callback]}]
           (let [url (get-in channel [:details :url])]
             (http/subscribe url id callback))))

(ns ticktok.subscriber
  (:require [ticktok.rabbit :as rabbit]
            [ticktok.http :as http]
            [ticktok.domain :as dom]))

(defprotocol Subscribble
  (subscribe [clock]))

(deftype HttpClock [id url callback]
  Subscribble
  (subscribe [clock]
    (http/subscribe (.url clock) (.id clock) (.callback clock))))

(deftype RabbitClock [id uri queue callback]
  Subscribble
  (subscribe [clock]
    (rabbit/subscribe (.id clock) (.uri clock) (.queue clock) (.callback clock))))

(defmulti clock (fn [{:keys [channel id]} _]
                      (keyword (:type channel))))

(defmethod clock :rabbit [{:keys [channel id]} {:keys [callback]}]
  (let [details (:details channel)]
    (RabbitClock. id (:uri details) (:queue details) callback)))

(defmethod clock :http [{:keys [channel id]} {:keys [callback]}]
  (let [url (get-in channel [:details :url])]
    (HttpClock. id url callback)))

(defn subscribe-clock [cl clock-req]
  (let [clock (clock cl clock-req)]
    (subscribe clock)))

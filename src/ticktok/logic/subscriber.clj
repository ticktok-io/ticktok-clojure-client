(ns ticktok.logic.subscriber
  (:require [ticktok.transport.rabbit :as rabbit]
            [ticktok.transport.http :as http]))

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

(defmulti clock-factory (fn [{:keys [channel]} _]
                          (keyword (:type channel))))

(defmethod clock-factory :rabbit [{:keys [channel id]} {:keys [callback]}]
  (let [details (:details channel)]
    (RabbitClock. id (:uri details) (:queue details) callback)))

(defmethod clock-factory :http [{:keys [channel id]} {:keys [callback]}]
  (let [url (get-in channel [:details :url])]
    (HttpClock. id url callback)))

(defn subscribe-clock [cl cl-req]
  (let [clock (clock-factory cl cl-req)]
    (subscribe clock)))

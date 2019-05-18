(ns ticktok.subscriber
  (:require [ticktok.rabbit :as rabbit]
            [ticktok.http :as http]
            [ticktok.domain :as dom]))

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


(defn clock-req
  ([n]
   (clock-req n #(println n " got tick")))
  ([n cb]
   {:name n
    :schedule "every.5.seconds"
    :callback cb}))

(def rabbit-channel {:details {:queue "queue"
                               :uri "rabbit"}
                     :type "rabbit"})

(def http-channel {:details {:url "http"}
                     :type "http"})

(defn clock-deb [{:keys [name schedule]} channel]
  {:channel channel
   :name name
   :schedule schedule
   :id "my.id"
   :url "my.url"})

(def rabbit-clock-req (clock-req "rabbit.clock"))

(def rabbit-clock (clock-deb rabbit-clock-req rabbit-channel))

(def http-clock-req (clock-req "http.clock"))

(def http-clock (clock-deb http-clock-req http-channel))

(def p-rabbit-clock (clock rabbit-clock rabbit-clock-req))

(def p-http-clock (clock http-clock http-clock-req))

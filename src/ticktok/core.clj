(ns ticktok.core
  (:require [ticktok.domain :as dom]
            [ticktok.rabbit :as rabbit]
            [ticktok.fetcher :refer [fetch-clock]]
            [ticktok.utils :refer [fail-with pretty validate-input]]))

(defn subscribe [clock clock-req]
  (let [q (get-in clock [:channel :queue])
        uri (get-in clock [:channel :uri])
        callback (:callback clock-req)]
    (rabbit/subscribe uri q callback)
    nil))

(defn make-clock [config]
  (fn [clock-request]
    (let [parsed-request (validate-input ::dom/clock-request clock-request)
          clock (fetch-clock (:host config) parsed-request)]
      (subscribe clock parsed-request)
      true)))

(defn ticktok [config]
  (let [parsed-config (validate-input ::dom/config config)]
    (make-clock parsed-config)))

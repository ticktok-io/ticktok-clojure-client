(ns ticktok.core
  (:require [org.httpkit.client :as http]
            [clojure.data.json :as json]
            [clojure.string :as string]
            [ticktok.domain :as dom]
            [clojure.spec.alpha :as s]
            [ticktok.rabbit :as rabbit]
            [ticktok.utils :refer [fail-with pretty validate-input]]))

(def api "/api/v1/clocks")

(defn fetch-clock [host clock-req]
  (let [options {:headers  {"Content-Type" "application/json"}
                 :body (json/write-str (select-keys clock-req [:name :schedule]))}
        endpoint (string/join [host api])
        {:keys [status body error]} @(http/post endpoint
                                                options)
        parse-clock (fn [raw]
                      (let [cl-map (json/read-str raw :key-fn keyword)
                            clock (dom/conform-clock cl-map)]
                        (if (= ::s/invalid clock)
                          (fail-with  "Failed to parse clock" {:clock raw})
                          clock)))]
    (if (not= status 201)
      (fail-with  "Failed to fetch clock" {:status status
                                           :request clock-req})
      (parse-clock body))))

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

(defn make-ticktok [config]
  (let [parsed-config (validate-input ::dom/config config)]
    (make-clock parsed-config)))

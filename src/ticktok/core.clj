(ns ticktok.core
  (:require [org.httpkit.client :as http]
            [clojure.data.json :as json]
            [clojure.string :as string]
            [ticktok.domain :as dom]
            [clojure.spec.alpha :as s]
            [ticktok.rabbit :as rabbit]))

(def api "/api/v1/clocks")

(defn fail-with
  ([msg]
   (fail-with msg {}))
  ([msg details]
   (println "error:" msg ", details:" details)
   (throw (ex-info msg details))))

(defn fetch-clock [host clock-req]
  (let [options {:headers  {"Content-Type" "application/json"}
                 :body (json/write-str (select-keys clock-req [:name :schedule]))}
        endpoint (string/join [host api])
        {:keys [status body error]} @(http/post endpoint
                                                options)
        parse-clock (fn [raw]
                      (let [cl-map (json/read-str raw :key-fn keyword)
                            cl (dom/conform-clock cl-map)]
                        (if (= ::s/invalid cl)
                          (fail-with  "Failed to parse clock" {:clock raw})
                          cl)))]
    ;(println "status " status)
    ;(println "error " error)
    ;(println "body " body)
    (if (not= status 201)
      (fail-with  "Failed to fetch clock" {:status status
                                           :request clock-req})
      (parse-clock body))))

(defn subscribe [clock clock-req]
  (let [q (get-in clock [:channel :queue])
        callback (:callback clock-req)]
    (println "subscribing " q)
    (rabbit/subscribe q callback)
    nil))

(defn ticktok [config clock-request]
  (let [parsed-config (dom/conform-config config)
        parsed-clock-request (dom/conform-clock-request clock-request)]
    (println "ticktok called: " parsed-config ", " parsed-clock-request)
    (cond
      (= ::s/invalid parsed-config) (dom/invalid-input ::dom/config config)
      (= ::s/invalid parsed-clock-request) (dom/invalid-input ::dom/clock-request clock-request)
      :else
      (let [clock (fetch-clock (:host parsed-config) parsed-clock-request)]
        (println "subscribing " clock)
        (subscribe clock parsed-clock-request)
        clock))))

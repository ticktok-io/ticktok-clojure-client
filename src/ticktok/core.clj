(ns ticktok.core
  (:require [org.httpkit.client :as http]
            [clojure.data.json :as json]
            [clojure.string :as string]
            [ticktok.domain :as dom]
            [clojure.spec.alpha :as s]))

(def api "/api/v1/clocks")

(defn get-clock [host clock-req]
  (let [options {:headers  {"Content-Type" "application/json"}
                 :body (json/write-str clock-req)}
        endpoint (string/join [host api])
        {:keys [status body error]} @(http/post endpoint
                                                options)]
    (println "status " status)
    (println "error " error)
    (println "body " body)
    (if (and (some? status) (= status 201))
      body
      false)))

(defn ticktok [config clock-request]
  (let [parsed-config (dom/conform-config config)
        parsed-clock-request (dom/conform-clock-request clock-request)]
    (println "config " parsed-config)
    (println "clock " parsed-clock-request)
    (cond
      (= ::s/invalid parsed-config) (dom/invalid-input ::dom/config config)
      (= ::s/invalid parsed-clock-request) (dom/invalid-input ::dom/clock-request clock-request)
      :else
      (get-clock (:host parsed-config) parsed-clock-request))))

(ns ticktok.core
  (:require [org.httpkit.client :as http]
           [clojure.data.json :as json]
           [clojure.string :as string]
           [ticktok.domain :as dom]))

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
  (cond
    (not (dom/valid-config? config)) (dom/invalid-input :dom/config config)
    (not (dom/valid-clock-request? clock-request)) (dom/invalid-input :dom/clock-request clock-request)
    :else
    (get-clock (:host config) clock-request)))

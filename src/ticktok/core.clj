(ns ticktok.core
  (:require [org.httpkit.client :as http]
           [clojure.data.json :as json]
           [clojure.string :as string]))

(def api "/api/v1/clocks")

(defn ticktok [host clock]
  (let [options {:headers  {"Content-Type" "application/json"}
                 :body (json/write-str clock)}
        endpoint (string/join [host api])
       {:keys [status body error]} @(http/post endpoint
                                               options)]
    (println "status " status)
    (println "error " error)
    (println "body " body)
    (println (and (some? status) (= status 404)))
    (if (and (some? status) (= status 404)) false nil)))

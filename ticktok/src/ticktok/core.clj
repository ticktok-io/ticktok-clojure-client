(ns ticktok.core
  (:gen-class)
  (require [org.httpkit.client :as http]
           [clojure.data.json :as json]
           [clojure.string :as string]))

(def api "/api/v1/clocks")

(defn ticktok [host clock]
  (let [options {:headers  {"Content-Type" "application/json"}
                 :body (json/write-str clock)}
        endpoint (string/join [host api])
       {:keys [status body error]} @(http/post endpoint
                       options)]
    (= status 201)))

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (println "Hello, World!"))

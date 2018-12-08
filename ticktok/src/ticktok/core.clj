(ns ticktok.core
  (:gen-class)
  (require [org.httpkit.client :as http]
           [clojure.data.json :as json]
           [clojure.string :as string]))


(defn ticktok [host clock]
  (println "going to send to " host " clock: " clock)
  (let [options {:headers  {"Content-Type" "application/json"}
                 :body (json/write-str clock)}
        endpoint (string/join host  "/api/v1/clocks")
       {:keys [status body error]} @(http/post endpoint
                       options)]
    (= status 201)))

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (println "Hello, World!"))

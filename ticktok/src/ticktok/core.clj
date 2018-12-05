(ns ticktok.core
  (:gen-class)
  (require [org.httpkit.client :as http]
           [clojure.data.json :as json]))


(defn ticktok [clock]
  (println clock)
  (http/post "http://localhost:8080/api/v1/clocks"
             {:headers  {"Content-Type" "application/json"}
              :body (json/write-str clock)}))

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (println "Hello, World!"))

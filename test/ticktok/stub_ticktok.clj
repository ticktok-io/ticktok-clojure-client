(ns ticktok.stub-ticktok
  (:require
            [compojure.core :refer :all]
            [compojure.handler :as handler]
            [compojure.route :refer :all]
            [ring.middleware.json :as middleware]
            [org.httpkit.server :as http]
            [clojure.core.async :as async :refer [chan put! <!!]]))


(defonce server (atom {:instance nil
                       :request nil
                       :response nil}))

(defn respond-with [server res]
  (swap! server assoc :response res)
  nil)

(defn clock-handler [req]
  (let [res (get @server :response)]
    (put! (get @server :request) req)
    (println "stub ticktok got" (:body req) "and respond with" res)
    res))

(defroutes api-routes
  (context "/api/v1/clocks" []
           (POST "/" [] clock-handler)))

(def app
  (-> (handler/site api-routes)
      (middleware/wrap-json-body {:keywords? true})))

(defn stop [server]
  (let [inst (get @server :instance)]
    (when-not (nil? inst)
      (inst :timeout 100)
      (swap! server assoc :instance nil :request nil :response nil)
      (println "stub ticktok stopped")
      nil)))

(defn start []
  (swap! server assoc :instance (http/run-server #'app {:port 8080}) :request (chan 1))
  (println "stub ticktok started")
  server)

(defn incoming-request [server]
  (let [c (get @server :request)
        req (<!! c)]
    req))

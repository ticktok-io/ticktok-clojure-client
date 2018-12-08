(ns ticktok.stub-ticktok
  (:require
            [compojure.core :refer :all]
            [compojure.handler :as handler]
            [compojure.route :refer :all]
            [ring.middleware.json :as middleware]
            [org.httpkit.server :as http]
            [clojure.core.async :as async :refer [chan dropping-buffer put! <! <!! go]]))


(defonce server (atom {:instance nil
                       :request (chan 1)}))


(defn clock-handler [req]
  (let [clock (:body req)]
    (println "stub ticktok got " clock)
    (swap! server update-in [:request] #(do
                                          (put! % clock)
                                          %))
    {:status 404}))

(defroutes api-routes
  (context "/api/v1/clocks" []
           (POST "/" [] clock-handler)))

(def app
  (-> (handler/site api-routes)
      (middleware/wrap-json-body {:keywords? true})))


(defn stop-server [server]
  (let [inst (get @server :instance)]
    (when-not (nil? inst)
      (inst :timeout 100)
      (swap! server assoc :instance nil)
      nil)))

(defn start-server []
  (swap! server assoc :instance (http/run-server #'app {:port 8080}))
  (println "statring stub server")
  server)

(defn incoming-request [server]
  (let [c (get @server :request)
        req (<!! c)]
    req))

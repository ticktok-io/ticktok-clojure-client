(ns ticktok.stub-ticktok
  (:require
            [compojure.core :refer :all]
            [compojure.handler :as handler]
            [compojure.route :refer :all]
            [ring.middleware.json :as middleware]
            [org.httpkit.server :as http]
            [clojure.core.async :as async :refer [chan dropping-buffer put! <! <!! go]]))


(defonce server (atom {:instance nil
                       :request nil}))

(defn stop []
  (let [inst (get @server :instance)]
    (when-not (nil? inst)
      (inst :timeout 100)
      (swap! server assoc :instance nil :request nil)
      nil)))

(defn start []
  (swap! server assoc :instance (http/run-server #'app {:port 8080}) :request (chan 1))
  (println "stub ticktok started")
  server)

(defn incoming-request []
  (let [c (get @server :request)
        req (<!! c)]
    req))

(defn clock-handler [req]
  (println "stub ticktok got " req)
  (swap! server update-in [:request] #(do
                                        (put! % req)
                                        %))
  {:status 404})

(defroutes api-routes
  (context "/api/v1/clocks" []
           (POST "/" [] clock-handler)))

(def app
  (-> (handler/site api-routes)
      (middleware/wrap-json-body {:keywords? true})))

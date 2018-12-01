(ns ticktok.core-test
  (:require [clojure.test :refer :all]
            [ticktok.core :refer :all]
            [compojure.core :refer :all]
            [compojure.route :refer :all]
            [cheshire.core :as json]
            [org.httpkit.server :as server]))

(defonce server (atom {:instance nil
                       :requests #{}}))

(defn clock-handler [req]
  (do
    (swap! server update-in conj req)
    {:status 404
     :headers {"Content-Type" "application/json"}}))

(defroutes api-routes
  (context "/api/v1/clocks" []
           (POST / [] clock-handler)))

(defn stop-server [server]
  (let [inst (get @server :instance)]
    (when-not (nil? inst)
    (inst :timeout 100)
    (swap! server assoc :instance nil)
    nil)))

(defn start-server []
  (swap! server assoc :instance (server/run-server #'api-routes {:port 8080}))
  server)

(defn incoming-requests [server]
  (get @server :requests))

(deftest fail-if-ticktok-server-not-found
  (testing "Should fail if ticktok server not found"
    (let [server (start-server)
          clock {:name "myclock"
                 :schedule "Every.5.Seconds"}
          result (ticktok clock)]
      (is (nil? result))
      (is (contains? (incoming-requests server) clock))
      (stop-server server))))

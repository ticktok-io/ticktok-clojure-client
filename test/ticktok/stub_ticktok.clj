(ns ticktok.stub-ticktok
  (:require
            [compojure.core :refer :all]
            [compojure.handler :as handler]
            [compojure.route :refer :all]
            [ring.middleware.json :as middleware]
            [org.httpkit.server :as http]
            [clojure.core.async :as async :refer [chan put! <!!]]
            [langohr.core      :as rmq]
            [langohr.channel   :as lch]
            [langohr.queue     :as lq]
            [langohr.consumers :as lc]
            [langohr.basic     :as lb]))

(defonce server (atom {:instance nil
                       :request nil
                       :response nil}))

(def ^{:const true}
  default-exchange-name "")

(defn message-handler
  [ch {:keys [content-type delivery-tag type] :as meta} ^bytes payload]
  (println (format "[consumer] Received a message: %s, delivery tag: %d, content type: %s, type: %s"
                   (String. payload "UTF-8") delivery-tag content-type type)))

(defn rabbit
  [& args]
  (let [conn  (rmq/connect)
        ch    (lch/open conn)
        qname "langohr.examples.hello-world"]
    (println (format "[main] Connected. Channel id: %d" (.getChannelNumber ch)))
    (lq/declare ch qname {:exclusive false :auto-delete true})
    (lc/subscribe ch qname message-handler {:auto-ack true})
    (lb/publish ch default-exchange-name qname "Hello!" {:content-type "text/plain" :type "greetings.hi"})
    (Thread/sleep 2000)
    (println "[main] Disconnecting...")
    (rmq/close ch)
    (rmq/close conn)))


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

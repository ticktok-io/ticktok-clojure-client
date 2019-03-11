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

(defonce rabbit (atom {:conn
                       :chan}))

(def ^{:const true}
  exchange-name "ticktok.fanout.ct")

(def qname "clock.ct")

(def rabbit-host "http://localhost:15672")

(defn start-rabbit []
  (let [conn  (rmq/connect)
        ch    (lch/open conn)]
    (swap! rabbit assoc :conn conn :chan ch)
    nil))

(defn stop-rabbit []
  (let [conn (:conn rabbit)
        ch (:chan rabbit)]
    (rmq/close ch)
    (rmq/close conn)
    (swap! rabbit assoc :conn nil :chan nil)
    nil))

(defn message-handler
  [ch {:keys [content-type delivery-tag type] :as meta} ^bytes payload]
  (println (format "[consumer] Received a message: %s, delivery tag: %d, content type: %s, type: %s"
                   (String. payload "UTF-8") delivery-tag content-type type)))

(defn send-tick []
  (lb/publish (:chan rabbit) exchange-name "" "my.tick" {:content-type "text/plain"}))

(defn subscribe [qname callback]
  (let [ch (:chan rabbit)
        handler    (fn [(:chan rabbit) {:keys [content-type delivery-tag type] :as meta} ^bytes payload]
                     (println (format "[consumer] received %s" (String. payload "UTF-8")))
                     (callback))]
    (lq/declare ch qname {:exclusive false :auto-delete true})
    (lq/bind ch qname exchange-name)
    (lc/subscribe ch qname handler {:auto-ack true}))
  nil)

(defn bind-queue [qname callback]
  (let [ch (:chan rabbit)]
    (le/declare ch exchange-name "fanout" {:durable false :auto-delete true})
    (subscribe qname callback)))

(defn make-clock-from [clock-req]
  (let [body {:channel {:queue qname
                        :uri rabbit-host}
              :name (:name clock-req)
              :schedule (:schedule clock-req)}]
    {:status 201
     :body (json/write-str body)}))

(defn respond-with [server res]
  (swap! server assoc :response res)
  nil)

(defn clock-handler [req]
  (let [res (get @server :response)]
    (put! (get @server :request) req)
    (bind-queue (get-in res [:channel :queue]) (:callback req))
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
      (stop-rabbit)
      (println "stub ticktok stopped")
      nil)))

(defn start []
  (start-rabbit)
  (swap! server assoc :instance (http/run-server #'app {:port 8080}) :request (chan 1))
  (println "stub ticktok started")
  server)

(defn incoming-request [server]
  (let [c (get @server :request)
        req (<!! c)]
    req))

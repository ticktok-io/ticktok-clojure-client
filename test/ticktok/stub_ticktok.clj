(ns ticktok.stub-ticktok
  (:require
            [compojure.core :refer :all]
            [compojure.handler :as handler]
            [compojure.route :refer :all]
            [ring.middleware.json :as middleware]
            [clojure.data.json :as json]
            [org.httpkit.server :as http]
            [clojure.core.async :as async :refer [chan put! <!!]]
            [langohr.core      :as rmq]
            [langohr.channel   :as lch]
            [langohr.queue     :as lq]
            [langohr.exchange  :as le]
            [langohr.consumers :as lc]
            [langohr.basic     :as lb]))

(defonce server (atom {:instance nil
                       :request nil
                       :response nil}))

(defonce rabbit (atom {:conn
                       :chan}))

(def exchange-name "ticktok.fanout.ct")

(def qname "clock.ct")

(def rabbit-host "http://localhost:15672")

(defn start-rabbit []
  (let [conn  (rmq/connect)
        ch    (lch/open conn)]
    (swap! rabbit assoc :conn conn :chan ch)
    (println "rabbit driver started")
    nil))

(defn stop-rabbit []
  (let [conn (:conn @rabbit)
        ch (:chan @rabbit)]
    (rmq/close ch)
    (rmq/close conn)
    (swap! rabbit assoc :conn nil :chan nil)
    (println "rabbit driver stopped")
    nil))

(defn send-tick []
  (lb/publish (:chan rabbit) exchange-name "" "my.tick" {:content-type "text/plain"})
  true)

(defn bind-queue [qname]
  (println "bind " qname)
  (let [ch (:chan @rabbit)]
    (le/declare ch exchange-name "fanout" {:durable false :auto-delete true})
    (println "exchange " exchange-name " created")
    (lq/declare ch qname {:exclusive false :auto-delete true})
    (println "queue " qname " created")
    (lq/bind ch qname exchange-name)
    (println "queue " qname " is bound to " exchange-name)
    nil))

(defn make-clock-from [clock-req]
  (let [body {:channel {:queue qname
                        :uri rabbit-host}
              :name (:name clock-req)
              :schedule (:schedule clock-req)
              :id "my.id"
              :url "my.url"}]
    {:status 201
     :body (json/write-str body)}))

(defn respond-with [server res]
  (swap! server assoc :response res)
  nil)

(defn schedule-ticks []
  (let [res (get @server :response)
        q (get-in res [:channel :queue])]    (bind-queue qname)
    nil))

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

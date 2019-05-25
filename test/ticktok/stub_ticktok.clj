(ns ticktok.stub-ticktok
  (:require
   [compojure.core :refer :all]
   [compojure.handler :as handler]
   [compojure.route :refer :all]
   [ring.middleware.json :as middleware]
   [clojure.data.json :as json]
   [org.httpkit.server :as http]
   [clojure.core.async :as async :refer [chan put! <!! close!]]
   [clojure.string :as string]
   [langohr.core      :as rmq]
   [langohr.channel   :as lch]
   [langohr.queue     :as lq]
   [langohr.exchange  :as le]
   [langohr.consumers :as lc]
   [langohr.basic     :as lb]
   [ticktok.utils :refer [pretty safe]]))

(defonce server (atom {:instance nil
                       :request nil
                       :response nil
                       :ticks #{}
                       :incoming-clocks #{}
                       :retry 0}))

(defonce rabbit (atom {:conn nil
                       :chan nil}))

(def exchange-name "ticktok.fanout.ct")

(def qname "clock.ct")

(def rabbit-uri "amqp://guest:guest@localhost:5672")

(def port 8080)

(def host (format "http://localhost:%d" port))

(defn rmq-chan []
  (:chan @rabbit))

(defn rmq-conn []
  (:conn @rabbit))

(defn rmq-chan-conn []
  [(rmq-chan), (rmq-conn)])

(defn not-running []
  (let [[chan conn] (rmq-chan-conn)]
    (every? nil? [chan conn])))

(defn running []
  (let [[chan conn] (rmq-chan-conn)]
    (every? some? [chan conn])))

(defn start-rabbit! []
  (when (not-running)
    (let [conn  (rmq/connect {:uri rabbit-uri})
          ch    (lch/open conn)]
      (swap! rabbit assoc :conn conn :chan ch)
      (println "rabbit stub started")))
  true)

(defn delete-exchange [name]
  (let [ch (rmq-chan)]
    (safe (le/delete ch name))))

(defn delete-queue [name]
  (let [ch (rmq-chan)]
    (safe (lq/delete ch name))))

(defn clear-resources! []
  (delete-queue qname)
  (println qname "deleted")
  (delete-exchange exchange-name)
  (println exchange-name "deleted")
  nil)

(defn close-rabbit! []
  (let [[chan conn] (rmq-chan-conn)
        closer #(when (rmq/open? %)
                  (rmq/close %))]
    (closer chan)
    (println "stub rabbit: channel closed")
    (closer conn)
    (println "stub rabbit: connection closed"))
  nil)

(defn stop-rabbit! []
  (when (running)
    (let [[chan conn] (rmq-chan-conn)
          closer #(when (rmq/open? %)
                    (rmq/close %))]
      (clear-resources!)
      (close-rabbit!)
      (swap! rabbit assoc :conn nil :chan nil)
      (println "rabbit stub stopped")))
  true)

(defn should-repond? []
  (let [retry (@server :retry)]
    (if (zero? retry)
      true
      (do
        (swap! server update-in [:retry] dec)
        false))))

(defn make-response
  ([body]
   (make-response body 201))
  ([body status]
   (let [resp {:status status
               :body (json/write-str body)}]
     resp)))

(defn make-clock-from
  ([clock-req]
   (make-clock-from clock-req qname))
  ([{:keys [name schedule]} qname]
   (let [body {:channel {:details {:queue qname
                                   :uri rabbit-uri}
                         :type "rabbit"}
               :name name
               :schedule schedule
               :id "my.id"
               :url "my.url"}]
     (make-response body))))

(defn clock-handler [req]
  (if (should-repond?)
    (let [res (@server :response)]
      (put! (@server :request) req)
      res)
    {:status 404}))

(defn http-clock-handler [clock-id]
  (println "popped for " clock-id)
  (swap! server update :incoming-clocks conj clock-id)
  (fn [req]
    (if (contains? (:ticks @server) clock-id)
      (do
        (swap! server update :ticks disj clock-id)
        (make-response [{:tick clock-id}] 200))
      (make-response nil 400))))

(defn tick-clock-handler [clock-id]
  (println "ticked for " clock-id)
  (swap! server update :incoming-clocks conj clock-id)
  (fn [req]
    (if (contains? (:ticks @server) clock-id)
      (do
        (swap! server update :ticks disj clock-id)
        (make-response nil 204))
      (make-response nil 400))))

(defroutes api-routes
  (context "/api/v1/clocks" []
           (POST "/" [access_token] clock-handler)
           (GET "/" [name schedule access_token] clock-handler))
  (GET "/:clock-id/pop" [clock-id]
       (http-clock-handler clock-id))
  (PUT "/:clock-id/tick" [clock-id]
       (tick-clock-handler clock-id)))

(def app
  (-> (handler/site api-routes)
      (middleware/wrap-json-body {:keywords? true})))

(defn start-server! []
  (swap! server assoc :instance (http/run-server #'app {:port 8080}) :request (chan 1) :retry  0)
  nil)

(defn stop-server! []
  (swap! server update-in [:request] close!)
  (swap! server assoc :instance nil :request nil :response nil :retry nil :ticks #{} :incoming-clocks #{})
  nil)

(defn stop! [server]
  (let [inst (get @server :instance)]
    (when-not (nil? inst)
      (inst :timeout 100)
      (stop-server!)
      (stop-rabbit!)
      (println "stub ticktok stopped")))
  nil)

(defn start! []
  (start-server!)
  (println "stub ticktok started")
  server)

(defn send-tick []
  (println "stub ticktok sending tick")
  (lb/publish (rmq-chan) exchange-name "" "my.tick" {:content-type "text/plain"})
  true)

(defn push-tick [server cl]
  (swap! server update :ticks conj cl)
  true)

(defn bind-queue [qname]
  (let [ch (rmq-chan)]
    (le/declare ch exchange-name "fanout" {:durable false :auto-delete true})
    (println "exchange " exchange-name " created")
    (lq/declare ch qname {:exclusive false :auto-delete true})
    (println "queue " qname " created")
    (lq/bind ch qname exchange-name)
    (println "queue " qname " is bound to " exchange-name))
  nil)

(defn respond-with [server res]
  (swap! server assoc :response res)
  nil)

(defn incoming-request [server]
  (let [c (@server :request)
        req (<!! c)]
    req))

(defn popped? [server clock]
  (contains? (:incoming-clocks @server) clock))

(defn fail-for [server n]
  (swap! server assoc :retry n)
  nil)

(defn schedule-ticks []
  (do
    (start-rabbit!)
    (bind-queue qname)
    (println "ticks are scheduled")
    nil))

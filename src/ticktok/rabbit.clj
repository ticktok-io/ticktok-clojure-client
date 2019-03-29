(ns ticktok.rabbit
  (:require [langohr.core      :as rmq]
            [langohr.channel   :as lch]
            [langohr.queue     :as lq]
            [langohr.consumers :as lc]
            [langohr.basic     :as lb]
            [ticktok.utils :refer [fail-with pretty]]))

(defonce rabbit (atom {:chan nil :conn nil}))

(defn- rmq-chan []
  (:chan @rabbit))

(defn- rmq-conn []
  (:conn @rabbit))

(defn- rmq-chan-conn []
  [(rmq-chan), (rmq-conn)])

(defn- not-running []
  (let [[chan conn] (rmq-chan-conn)]
    (every? nil? [chan conn])))

(defn- running []
  (let [[chan conn] (rmq-chan-conn)]
    (every? some? [chan conn])))

(defn start! [uri]
  (when (not-running)
    (let [conn  (rmq/connect {:uri uri})
          ch    (lch/open conn)]
      (swap! rabbit assoc :conn conn :chan ch)
      (println "rabbit prod started")))
  nil)

(defn stop! []
  (when (running)
    (let [[chan conn] (rmq-chan-conn)
          closer #(when (rmq/open? %)
                    (rmq/close %))]
      (closer chan)
      (closer conn)
      (swap! rabbit assoc :conn nil :chan nil)
      (println "rabbit prod stopped")))
  nil)

(defn- exception-handler [e details msg]
  (let [exp (Throwable->map e)
        explain (merge details {:error (:cause exp)})]
    (stop!)
    (fail-with msg explain)))

(defn- wrap [callback]
  (fn [ch {:keys [content-type delivery-tag type] :as meta} ^bytes payload]
    (let [msg (String. payload "UTF-8")
          r (callback)]
      (println (format "[consumer] received %s, returned %s" msg r))
      r)))

(defmacro try-or-fail [action req msg]
  `(try
     ~action
     (catch Exception e#
       (exception-handler e# ~req ~msg))))

(defn- try-subscribe [qname callback]
  (try-or-fail
   (lc/subscribe (rmq-chan) qname (wrap callback) {:auto-ack true})
   {:queue qname}
   "Failed to subscribe queue"))

(defn- try-connect [uri]
  (try-or-fail
   (start! uri)
   {:uri uri}
   "Failed to connect queue server"))

(defn subscribe [uri qname callback]
  (try-connect uri)
  (try-subscribe qname callback))

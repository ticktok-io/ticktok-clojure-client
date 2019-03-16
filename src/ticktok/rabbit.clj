(ns ticktok.rabbit
  (:require [langohr.core      :as rmq]
            [langohr.channel   :as lch]
            [langohr.queue     :as lq]
            [langohr.consumers :as lc]
            [langohr.basic     :as lb]
            [ticktok.utils :refer [fail-with]]))

(defonce rabbit (atom {:chan nil :conn nil}))

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
    (let [conn  (rmq/connect)
          ch    (lch/open conn)]
      (swap! rabbit assoc :conn conn :chan ch)
      (println "rabbit prod started")
      true)
    true))

(defn stop-rabbit! []
  (when (running)
    (let [[chan conn] (rmq-chan-conn)
          closer #(when (rmq/open? %)
                    (rmq/close %))]
      (closer chan)
      (closer conn)
      (swap! rabbit assoc :conn nil :chan nil)
      (println "rabbit prod stopped")
      true))
  true)


(defn subscribe [qname callback]
  (let [handler (fn [ch {:keys [content-type delivery-tag type] :as meta} ^bytes payload]
                  (let [msg (String. payload "UTF-8")
                        r (callback)]
                    (println (format "[consumer] received %s" msg))
                    (println "callback returned " r)))]
    (try
      (do
        (start-rabbit!)
        (println "going to subscribe " qname)
        (lc/subscribe (rmq-chan) qname handler {:auto-ack true}))
      (catch Exception e
        (fail-with "Failed to subscribe queue" {:queue qname
                                                :error (.getMessage e)})))))

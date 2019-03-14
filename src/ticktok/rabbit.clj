(ns ticktok.rabbit
  (:require [langohr.core      :as rmq]
            [langohr.channel   :as lch]
            [langohr.queue     :as lq]
            [langohr.consumers :as lc]
            [langohr.basic     :as lb]))

(defonce rabbit (atom {:chan nil :conn nil}))

(defn not-running []
  (let [{:keys [chan conn]} @rabbit]
    (every? nil? [chan conn])))

(defn running []
  (let [{:keys [chan conn]} @rabbit]
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
    (let [{:keys [conn chan]} @rabbit]
      (println (rmq/close chan))
      (println (rmq/close conn))
      (swap! rabbit assoc :conn nil :chan nil)
      (println "rabbit prod stopped")
     true))
  true)

(defn rabbit-channel []
  (do
    (start-rabbit!)
    (:chab @rabbit)))

(defn subscribe [qname callback]
  (let [ch (rabbit-channel)
        handler (fn [ch {:keys [content-type delivery-tag type] :as meta} ^bytes payload]
                     (println (format "[consumer] received %s" (String. payload "UTF-8")))
                     (callback))]
    (lc/subscribe ch qname handler {:auto-ack true})
    nil))

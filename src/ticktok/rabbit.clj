(ns ticktok.rabbit
  (:require [langohr.core      :as rmq]
            [langohr.channel   :as lch]
            [langohr.queue     :as lq]
            [langohr.consumers :as lc]
            [langohr.basic     :as lb]))


(defn get-rabbit-channel []
  (let [conn (rmq/connect)
        ch (lch/open conn)]
    ch))

(defn subscribe [qname callback]
  (let [ch (get-rabbit-channel)
        handler (fn [ch {:keys [content-type delivery-tag type] :as meta} ^bytes payload]
                     (println (format "[consumer] received %s" (String. payload "UTF-8")))
                     (callback))]
    (lc/subscribe ch qname handler {:auto-ack true})
    nil))

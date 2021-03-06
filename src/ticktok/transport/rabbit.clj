(ns ticktok.transport.rabbit
  (:require [langohr.core      :as rmq]
            [langohr.channel   :as lch]
            [langohr.consumers :as lc]
            [ticktok.utils :refer [fail-with]]))

(defonce rabbit (atom {:chan nil :conn nil}))

(defonce state (atom {:clocks {}}))

(defn- clocks []
  (:clocks @state))

(defn- rmq-chan []
  (:chan @rabbit))

(defn- rmq-conn []
  (:conn @rabbit))

(defn- rmq-chan-conn []
  [(rmq-chan), (rmq-conn)])

(defn- not-running []
  (let [[chan conn] (rmq-chan-conn)]
    (every? nil? [chan conn])))

(defn start! [uri]
  (when (not-running)
    (let [conn  (rmq/connect {:uri uri})
          ch    (lch/open conn)]
      (swap! rabbit assoc :conn conn :chan ch)))
  nil)

(defn- close-rabbit! []
  (let [[chan conn] (rmq-chan-conn)
        closer #(when (and (some? %) (rmq/open? %))
                  (rmq/close %))]
    (closer chan)
    (closer conn)
    (swap! rabbit assoc :conn nil :chan nil)
    nil))

(defn stop! []
  (close-rabbit!)
  (swap! state assoc :clocks {})
  nil)

(defn- exception-handler [e details msg]
  (let [exp (Throwable->map e)
        explain (merge details {:error (:cause exp)})]
    (stop!)
    (fail-with msg explain)))

(defn- wrap [callback]
  (fn [ch {:keys [content-type delivery-tag type] :as meta} ^bytes payload]
    (let [msg (String. payload "UTF-8")
          r (@callback)]
      r)))

(defmacro try-or-fail [action req msg]
  `(try
     ~action
     (catch Exception e#
       (exception-handler e# ~req ~msg))))

(defn swap-callback! [cb new-cb]
  (reset! cb new-cb))

(defn- subscribe-callback! [id qname callback]
  (let [callback-ref (atom callback)]
    (lc/subscribe (rmq-chan) qname (wrap callback-ref) {:auto-ack true})
    (swap! state update :clocks assoc id callback-ref)))

(defn- subscribe-queue [id qname callback]
  (if-let [cb (get (clocks) id)]
    (swap-callback! cb callback)
    (subscribe-callback! id qname callback)))

(defn- try-subscribe [id qname callback]
  (try-or-fail
   (subscribe-queue id qname callback)
   {:queue qname}
   "Failed to subscribe queue"))

(defn- try-connect [uri]
  (try-or-fail
   (start! uri)
   {:uri uri}
   "Failed to connect queue server"))

(defn subscribe [id uri qname callback]
  (try-connect uri)
  (try-subscribe id qname callback))

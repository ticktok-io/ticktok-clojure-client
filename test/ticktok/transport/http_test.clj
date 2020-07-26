(ns ticktok.transport.subscribe-http-test
  (:require [clojure.test :refer :all]
            [ticktok.transport.http :as http]
            [ticktok.stub-ticktok :as stub]
            [clojure.core.async :as async :refer [chan put! <!! close!]]
            [midje.sweet :refer :all]))

(def state (atom {:stub-ticktok nil}))

(def host  "http://localhost:8080")

(defn clock [name]
  (format "%s/%s/pop" host name))

(defn start-ticktok []
  (swap! state assoc :stub-ticktok (stub/start!)))

(defn stop-ticktok []
  (swap! state update :stub-ticktok stub/stop!))

(defn stub-ticktok []
  (get @state :stub-ticktok))

(defn subscribe [ck callback]
  (http/subscribe (clock ck) ck callback)
  true)

(defn popped? [clock]
  (stub/popped? (stub-ticktok) clock))

(defn wait-a-bit []
  (Thread/sleep 3000)
  true)

(defn push-tick [& cs]
  (let [stub (stub-ticktok)]
    (doseq [c cs]
      (stub/push-tick stub c)))
  true)

(facts :f "about subscribing to clock on http mode"

       (with-state-changes [(before :contents (start-ticktok))
                            (after :contents (stop-ticktok))]

         (let [counter (atom 0)
               not-invoked? #(zero? @counter)
               callback #(swap! counter inc)]

           (with-state-changes [(after :contents (reset! counter 0))]

             (fact "should not invoke callback when failed to fetch ticks"
                   (subscribe "c0" callback) => truthy
                   (wait-a-bit) => true
                   (popped? "c0") => true
                   (not-invoked?) => true)

             (fact "should stop subscribing for all consumers"
                   (subscribe "c1" callback) => truthy
                   (subscribe "c2" callback) => truthy
                   (http/stop!) => truthy
                   (push-tick "c1" "c2") => true
                   (not-invoked?) => true)))

         (with-state-changes [(after :contents (http/stop!))]

           (fact "should replace callback for given clock"

                 (let [ch (chan 1)
                       cb1 #(put! ch "cb1")
                       cb2 #(put! ch "cb2")
                       invoked? (fn [cb]
                                  (let [m (<!! ch)]
                                    (= m cb)))]

                   (subscribe "c3" cb1) => true
                   (push-tick "c3") => true
                   (invoked? "cb1") => true
                   (subscribe "c3" cb2) => true
                   (push-tick "c3") => true
                   (invoked? "cb2") => true
                   (push-tick "c3") => true
                   (invoked? "cb1") => false
                   )))))

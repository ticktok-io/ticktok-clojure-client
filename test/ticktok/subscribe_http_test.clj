(ns ticktok.subscribe-http-test
  (:require [clojure.test :refer :all]
            [ticktok.http :as http]
            [ticktok.stub-ticktok :as stub]
            [clojure.core.async :as async :refer [chan put! <!! close!]]
            [midje.sweet :refer :all]))

(def state (atom {:stub-ticktok nil}))

(def clock-name "my.clock")

(def host  "http://localhost:8080")

(defn clock [name]
  (format "%s/%s/pop" host name))

(def clock-url (clock clock-name))

(defn start-ticktok []
  (swap! state assoc :stub-ticktok (stub/start!)))

(defn stop-ticktok []
  (swap! state update :stub-ticktok stub/stop!))

(defn stub-ticktok []
  (get @state :stub-ticktok))

(defn subscribe [ck callback]
  (http/subscribe (clock ck) ck callback)
  true)

(defn ticktok-scheduled-ticks []
  (start-ticktok)
  (stub/respond-with (stub-ticktok) (stub/make-request [{:tick "1"}] 200)))

(defn popped? [clock]
  (stub/popped? (stub-ticktok) clock))

(defn wait-a-bit []
  (Thread/sleep 3000)
  true)

(defn push-ticks [& cs]
  (let [stub (stub-ticktok)]
    (doseq [c cs]
      (stub/push-tick stub c)))
  true)


(facts "about subscribing to clock on http mode"


       (with-state-changes [(before :contents (start-ticktok))
                            (after :contents (stop-ticktok))]

         (fact :f "should not invoke callback failed to fetch ticks"
               (let [counter (atom 0)
                     not-invoked? #(zero? @counter)
                     callback #(swap! counter inc)
                     clock "c0"]
                 (subscribe clock callback) => truthy
                 (wait-a-bit) => true
                 (popped? clock) => true
                 (not-invoked?) => true
                 ))

         (fact :f "should stop subscribing for all consumers"
               (let [counter (atom 0)
                     not-invoked? #(zero? @counter)
                     callback #(swap! counter inc)]
                 (subscribe "c1" callback) => truthy
                 (subscribe "c2" callback) => truthy
                 (http/stop!) => truthy
                 (push-ticks "c1" "c2") => true
                 (not-invoked?) => true))

         (with-state-changes [(after :contents (http/stop!))]

           (fact "should invoke callback upon tick"
                 (let [ch (chan 1)
                       callback #(put! ch "got tick")
                       invoked? #(let [m (<!! ch)]
                                   (close! ch)
                                   m)]
                   (subscribe "c3" callback) => true
                   (push-ticks "c3")
                   (invoked?) => truthy
                   )))))

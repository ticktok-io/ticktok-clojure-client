(ns ticktok.subscribe-test
  (:require [clojure.test :refer :all]
            [ticktok.rabbit :refer [subscribe stop! start!]]
            [ticktok.stub-ticktok :as stub]
            [clojure.core.async :as async :refer [chan put! <!! close!]]
            [midje.sweet :refer :all]))

(def rabbit-uri "amqp://guest:guest@localhost:5672")

(def invalid-uri "amqp://guest:nonguest@localhost:5672/nonhost")

(defn subscribe-queue
  ([uri]
   (subscribe-queue uri ""))
  ([uri qname]
   (subscribe-queue uri qname #()))
  ([uri qname callback]
   (subscribe "fake-id" uri qname callback))
  ([id uri qname callback]
   (println "subscribing " id " to " qname " on " uri)
   (subscribe id uri qname callback)))

(defn create-queues [& qs]
  (doseq [q qs]
    (stub/bind-queue q)))

(defn delete-queues [& qs]
  (doseq [q qs]
    (stub/delete-queue q)))

(defn wait-a-bit []
  (Thread/sleep 3000)
  true)

(facts "about subscribing to queue"

       (facts "when successfully subscribed"

              (with-state-changes [(after :contents (stop!))]

                (with-state-changes [(before :contents (do
                                                         (stub/start-rabbit!)
                                                         (create-queues "q1" "q2")))
                                     (after :contents (do
                                                        (delete-queues "q1" "q2")
                                                        (stub/stop-rabbit!)))]

                  (fact "should close rabbit for all consumers"

                        (let [counter (atom 0)
                              not-invoked? #(zero? @counter)
                              callback #(swap! counter inc)]

                          (subscribe-queue rabbit-uri "q1" callback) => truthy
                          (subscribe-queue rabbit-uri "q2" callback) => truthy
                          (stop!) => nil
                          (stub/send-tick) => true
                          (wait-a-bit) => true
                          (not-invoked?) => true)))

                (with-state-changes [(before :contents (do
                                                         (stub/start-rabbit!)
                                                         (create-queues "q1")))
                                     (after :contents (do
                                                        (delete-queues "q1")
                                                        (stub/stop-rabbit!)))]

                  (fact "should replace callback for given clock"

                        (let [ch (chan 1)
                              cb1 #(put! ch "cb1")
                              cb2 #(put! ch "cb2")
                              invoked? (fn [cb]
                                         (let [m (<!! ch)]
                                           (= m cb)))]
                          (subscribe-queue "id-1234" rabbit-uri "q1" cb1) => truthy
                          (stub/send-tick) => true
                          (invoked? "cb1") => true
                          (subscribe-queue "id-1234" rabbit-uri "q1" cb2) => truthy
                          (stub/send-tick) => true
                          (invoked? "cb2") => true
                          (stub/send-tick) => true
                          (invoked? "cb1") => false
                          )))))

       (facts "when failed to subscribe"

              (with-state-changes [(after :contents (stop!))]

                (fact "should fail for connection error"
                      (subscribe-queue invalid-uri)) => (throws RuntimeException #"Failed to connect queue server")

                (fact "should fail if queue wasn't found"
                      (subscribe-queue rabbit-uri "invalid.q")) => (throws RuntimeException #"Failed to subscribe queue"))))

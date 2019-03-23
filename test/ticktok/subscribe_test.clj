(ns ticktok.subscribe-test
  (:require [clojure.test :refer :all]
            [clojure.string :as string]
            [ticktok.rabbit :refer [subscribe]]
            [ticktok.utils :refer [pretty]]
            [ticktok.stub-ticktok :as stub]
            [midje.sweet :refer :all]
            [clojure.data.json :as json]
            [clojure.core.async :as async :refer [chan put! <!! close!]]
            [ticktok.domain :as dom]))

(def rabbit-uri "amqp://guest:guest@localhost:5672")

(def invalid-uri "amqp://guest:guest@localhost:5672/someHost")

(defn subscribe-queue
  ([uri]
   (subscribe-queue uri ""))
  ([uri qname]
   (subscribe-queue uri qname #()))
  ([uri qname callback]
   (subscribe uri qname callback)
   true))

(facts "about subscribing to queue"
       (facts "when failed to subscribe"

              (fact "should fail for connection error"
                    (subscribe-queue invalid-uri)) => (throws RuntimeException #"Failed to connect queue server")

              (fact "should fail if queue wasn't found"
                    (subscribe-queue rabbit-uri "invalid.q")) => (throws RuntimeException #"Failed to subscribe queue"))

       (facts "when successfully subscribed"

              (with-state-changes [(before :facts (stub/schedule-ticks))
                                   (after :facts (stub/stop-rabbit!))]
                (let [ch (chan 1)
                      callback #(put! ch "got tick")
                      is-inovked #(let [m (<!! ch)]
                                    (close! ch)
                                    m)]

                  (fact "callback should invoked upon tick"
                        (subscribe-queue rabbit-uri stub/qname callback) => true
                        (stub/send-tick) => true
                        (is-inovked) => truthy
                        )))))

(ns ticktok.subscribe-test
  (:require [clojure.test :refer :all]
            [clojure.string :as string]
            [ticktok.rabbit :refer [subscribe]]
            [midje.sweet :refer :all]))

(def rabbit-uri "amqp://guest:guest@localhost:5672")

(def invalid-uri "amqp://guest:guest@localhost:5672/someHost")

(defn subscribe-queue
  ([uri]
   (subscribe-queue uri ""))
  ([uri qname]
   (subscribe uri qname #())
   true))

(facts :f "about subscribing to queue"
       (facts "when failed to subscribe"

              (fact "should fail for connection error"
                    (subscribe-queue invalid-uri)) => (throws RuntimeException #"Failed to connect queue server")

              (fact "should fail if queue wasn't found"
                    (subscribe-queue rabbit-uri "invalid.q")) => (throws RuntimeException #"Failed to subscribe queue")))

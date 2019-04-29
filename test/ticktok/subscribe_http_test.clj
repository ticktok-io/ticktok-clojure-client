(ns ticktok.subscribe-http-test
  (:require [clojure.test :refer :all]
            [ticktok.http :refer [subscribe]]
            [ticktok.stub-ticktok :as stub]
            [midje.sweet :refer :all]))

(facts :f "about subscribing to clock on http mode"

       (facts "when failed to connect"
              (with-state-changes [(after :contents (stop!))]

                (fact "should ignore pfor connection error"
                      (subscribe-queue invalid-uri)) => (throws RuntimeException #"Failed to connect queue server"))))

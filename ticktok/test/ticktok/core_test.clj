(ns ticktok.core-test
  (:require [clojure.test :refer :all]
            [ticktok.core :refer :all]
            [ticktok.stub-ticktok :as stub]))

(deftest componenet
  (let [stub-ticktok (stub/start-server)
        endpoint ]
    (testing "Should fail if ticktok server not found"
    (let [clock {:name "myclock"
                 :schedule "Every.5.Seconds"}
          result (ticktok clock)]
      (is (false? result))
      (is (stub/incoming-request stub-ticktok) clock)))
    (stub/stop-server stub-ticktok)))

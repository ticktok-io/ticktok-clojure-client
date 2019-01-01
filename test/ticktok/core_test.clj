(ns ticktok.core-test
  (:require [clojure.test :refer :all]
            [ticktok.core :refer :all]
            [ticktok.stub-ticktok :as stub]))

(def host  "http://localhost:8080")

(deftest component
  (testing "Should fail if ticktok server not found"
    (let [stub-ticktok (stub/start)
          clock {:name "myclock"
                 :schedule "Every.5.Seconds"}]
      (stub/respond-with stub-ticktok {:status 404})
      (is (false? (ticktok host clock)))
      (is (:body (stub/incoming-request stub-ticktok)) clock)
      (stub/stop stub-ticktok))))

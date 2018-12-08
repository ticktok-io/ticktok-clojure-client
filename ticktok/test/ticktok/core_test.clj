(ns ticktok.core-test
  (:require [clojure.test :refer :all]
            [ticktok.core :refer :all]
            [ticktok.stub-ticktok :as stub]))

(defn with-ticktok-server [f]
  (stub/start-server)
  (f)
  (stub/stop-server))

(use-fixtures :once with-ticktok-server)

(deftest componenet
  (testing "Should fail if ticktok server not found"
    (let [host "http://localhost:8080"
          clock {:name "myclock"
                 :schedule "Every.5.Seconds"}
          result (ticktok host clock)]
      (is (false? result))
      (is (stub/incoming-request) clock))))
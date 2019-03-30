(ns ticktok.validation-test
  (:require [clojure.test :refer :all]
            [ticktok.core :as tk]
            [midje.sweet :refer :all]))

(def valid-host  "http://localhost:8080")

(defn register-clock [conf req]
  (tk/ticktok :schedule conf req)
  true)

(facts "about clock validity"

       (tabular
        (fact "should fail for invalid clock request"
              (register-clock ?host ?clock)) => (throws RuntimeException #"Invalid input")

        ?host ?clock
        valid-host {:name "my.clock"}
        valid-host {:name "my.clock" :schedule "Every.3.seconds"}
        valid-host {:name "my.clock" :schedule "every.0.seconds"}
        "" {:name "my.clock" :schedule "every.3.seconds"}
        ))

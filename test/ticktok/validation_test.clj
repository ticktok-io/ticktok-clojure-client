(ns ticktok.validation-test
  (:require [clojure.test :refer :all]
            [ticktok.core :as tk]
            [midje.sweet :refer :all]))

(def valid-config  {:host "some.host"
                    :token "some.token"})

(def valid-clock {:name "my.clock"
                  :schedule "every.3.seconds"})

(defn register-clock [conf req]
  (tk/ticktok :schedule conf req)
  true)

(facts "about clock validity"

       (tabular
        (fact "should fail for invalid clock request"
              (register-clock ?host ?clock)) => (throws RuntimeException #"Invalid input")

        ?host ?clock
        valid-config {:name "my.clock"}
        valid-config {:schedule "every.3.seconds"}
        {:host "host"} valid-clock
        {:token "token"} valid-clock
        ))

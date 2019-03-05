(ns ticktok.core-test
  (:require [clojure.test :refer :all]
            [ticktok.core :refer :all]
            [ticktok.stub-ticktok :as stub]
            [midje.sweet :refer :all])
  )

(def host  "http://localhost:8080")

(def state (atom {:stub-ticktok nil}))

(defn start-ticktok []
  (swap! state assoc :stub-ticktok (stub/start)))

(defn stop-ticktok []
  (swap! state update :stub-ticktok stub/stop))

(defn stub-ticktok []
  (get @state :stub-ticktok))

(defn stub-ticktok-is-not-found []
  (stub/respond-with (stub-ticktok) {:status 404}))

(facts "about ticktok"
       (with-state-changes [(before :facts (start-ticktok))
                            (after :facts (stop-ticktok))]
         (fact "should fail if ticktok server not found"
               (prerequisite (stub-ticktok-is-not-found)) => nil)
               (let [clock {:name "myclock"
                            :schedule "Every.5.Seconds"}]
                 (ticktok host clock) => false
                 (:body (stub/incoming-request (stub-ticktok))) => clock))))

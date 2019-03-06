(ns ticktok.core-test
  (:require [clojure.test :refer :all]
            [ticktok.core :refer :all]
            [ticktok.domain :refer :all]
            [ticktok.stub-ticktok :as stub]
            [midje.sweet :refer :all]
            [clojure.data.json :as json])
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

(defn make-clock-request []
  {:name "myclock"
   :schedule "Every.5.Seconds"})

(defn make-clock-from [clock-req]
  (let [body {::channel {::queue "queue.it"
                         ::uri "rabbit.uri"}
              ::name (:name clock-req)
              ::schedule (:schedule clock-req)}]
    {:status 201
     :body (json/write-str body)}))


(facts "about ticktok"
       (with-state-changes [(before :facts (start-ticktok))
                            (after :facts (stop-ticktok))]
         (let [clock-request (make-clock-request)]
           (fact "should fail if ticktok server not found"
                 (prerequisite (stub-ticktok-is-not-found) => nil)
                 (ticktok host clock-request) => false
                 (:body (stub/incoming-request (stub-ticktok))) => clock-request)
           (comment (fact "should return clock details"
                          (let [clock (make-clock-from clock-request)]
                            (prerequisites (stub/respond-with (stub-ticktok) clock) => nil)
                            (ticktok host clock-request) => (:body clock)))
))))

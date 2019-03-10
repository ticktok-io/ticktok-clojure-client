(ns ticktok.core-test
  (:require [clojure.test :refer :all]
            [ticktok.core :refer :all]
            [ticktok.stub-ticktok :as stub]
            [midje.sweet :refer :all]
            [clojure.data.json :as json])
  )

(def host  "http://localhost:8080")

(def state (atom {:stub-ticktok nil}))

(def config {:host host :token "my.token"})


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
   :schedule "every.5.seconds"})

(defn make-clock-from [clock-req]
  (let [body {::channel {::queue "queue.it"
                         ::uri "rabbit.uri"}
              ::name (:name clock-req)
              ::schedule (:schedule clock-req)}]
    {:status 201
     :body (json/write-str body)}))


(facts :e2e "about ticktok"
       (with-state-changes [(before :contents (start-ticktok))
                            (after :contents (stop-ticktok))]
         (against-background [(before :facts (stub-ticktok-is-not-found))]
                             (fact "should fail if ticktok server not found"
                                   (let [clock-request (make-clock-request)]
                                     (ticktok config clock-request) => false
                                     (:body (stub/incoming-request (stub-ticktok))) => clock-request)))
         (let [clock-request (make-clock-request)
               clock (make-clock-from clock-request)]
           (against-background [(before :facts (stub/respond-with (stub-ticktok) clock))]
                               (fact "should return clock details"
                                     (ticktok config clock-request) => (:body clock))))))
(facts :unit "about clock validity"
       (tabular
        (fact :unit "should return fail for invalid clock request"
              (ticktok ?host ?clock)) => (throws Exception)
        ?host ?clock
        "" {}
        ))

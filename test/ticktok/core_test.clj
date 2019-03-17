(ns ticktok.core-test
  (:require [clojure.test :refer :all]
            [clojure.string :as string]
            [ticktok.core :refer :all]
            [ticktok.utils :refer [pretty]]
            [ticktok.stub-ticktok :as stub]
            [midje.sweet :refer :all]
            [clojure.data.json :as json]
            [clojure.core.async :as async :refer [chan put! <!! close!]]))

(def host  "http://localhost:8080")

(def state (atom {:stub-ticktok nil}))

(def config {:host host :token "my.token"})

(defn start-ticktok []
  (swap! state assoc :stub-ticktok (stub/start!)))

(defn stop-ticktok []
  (swap! state update :stub-ticktok stub/stop!))

(defn stub-ticktok []
  (get @state :stub-ticktok))

(defn stub-respond-with [resp]
  (stub/respond-with (stub-ticktok) resp))

(defn make-clock-request
  ([]
   (make-clock-request #()))
  ([callback]
   (let [req {:name "myclock"
              :schedule "every.5.seconds"
              :callback callback}]
     req)))

(def clock-request (make-clock-request))

(def clock (stub/make-clock-from clock-request))

(defn stub-ticktok-incoming-request []
  (:body (stub/incoming-request (stub-ticktok))))

(defn stub-ticktok-respond-with-clock []
  (println "stub-ticktok-respond-with-clock")
  (stub-respond-with clock)
  true)

(defn stub-ticktok-is-not-found []
  (println "stub-ticktok-is-not-found")
  (stub-respond-with {:status 404})
  true)

(defn stub-ticktok-respond-with-invalid-clock []
  (println "stub-ticktok-respond-with-invalid-clock")
  (stub-respond-with (stub/make-clock-from {}))
  true)

(defn stub-ticktok-respond-with-clock-and-schedule-ticks [clock]
  (do
    (stub-respond-with clock)
    (stub/schedule-ticks)
    true))

(defn clock-from [clock-req]
  (select-keys clock-req [:name :schedule]))


(facts :f "when ticktok failed to fetch clock"
       (with-state-changes [(before :contents (start-ticktok))
                            (after :contents (stop-ticktok))]
         (facts "when ticktok server failed to respond"
                (with-state-changes [(before :contents (stub-ticktok-is-not-found))]
                  (fact "should fail if ticktok server not found"
                        (ticktok config clock-request)) => (throws RuntimeException #"Failed to fetch clock" #(= (:status (ex-data %)) 404))
                  (fact "should ask from ticktok server clock"
                        (stub-ticktok-incoming-request) => (clock-from clock-request)))
                (with-state-changes [(before :contents (stub-ticktok-respond-with-invalid-clock))]
                  (fact "should fail if ticktok server respond with invalid clock"
                        (ticktok config clock-request)) => (throws RuntimeException #"Failed to parse clock" #(contains? (ex-data %) :clock)))
                (with-state-changes [(before :contents (stub-ticktok-respond-with-clock))]
                  (fact "should fail if failed to connect to rabbit"
                        (ticktok config clock-request)) => (throws RuntimeException #"Failed to subscribe queue" #(contains? (ex-data %) :queue))
                  (fact "should fail if queue wasn't found"
                        (ticktok config clock-request)) => (throws RuntimeException #"Failed to subscribe queue" #(string/includes? (:error (ex-data %)) "NOT_FOUND"))))
         ))

(facts :s "when clock is successfully sent"
       (with-state-changes [(before :contents (start-ticktok))
                            (after :contents (stop-ticktok))]
         (with-state-changes [(before :facts(stub-ticktok-respond-with-clock-and-schedule-ticks clock))                              ]
           (let [ch (chan 1)
                 clock-request (make-clock-request #(let []
                                                      (println "i got tick")
                                                      (put! ch "got tick")
                                                      true))
                 clock (stub/make-clock-from clock-request)
                 is-inovked #(let [m (<!! ch)]
                               (println "msg is" m)
                               (close! ch)
                               (not= nil m))]
             (fact "should invoke callback upon tick"
                   (ticktok config clock-request) => true
                   (stub/send-tick) => true
                   (is-inovked) => true
                   )))))

(comment (facts :unit "about clock validity"
                (tabular
                 (fact :unit "should return fail for invalid clock request"
                       (ticktok ?host ?clock)) => (throws RuntimeException #"")
                 ?host ?clock
                 "" {}
                 )))

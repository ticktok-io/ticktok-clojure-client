(ns ticktok.core-test
  (:require [clojure.test :refer :all]
            [ticktok.core :refer [ticktok]]
            [ticktok.stub-ticktok :as stub]
            [midje.sweet :refer :all]
            [clojure.core.async :as async :refer [chan put! <!! close!]]
            [ticktok.domain :as dom]))

(def host  "http://localhost:8080")

(def state (atom {:stub-ticktok nil}))

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

(def config {:host host :token "my.token"})

(defn stub-ticktok-incoming-request []
  (:body (stub/incoming-request (stub-ticktok))))

(defn stub-ticktok-returned-bad-request []
  (println "stub-ticktok-returned-bad-request")
  (stub-respond-with {:status 400})
  true)

(defn stub-ticktok-respond-with-clock-and-schedule-ticks [clock]
  (do
    (stub-respond-with clock)
    (stub/schedule-ticks)
    true))

(defn register-clock
  ([]
   (register-clock clock-request))
  ([req]
   (register-clock config req))
  ([conf req]
   (let [ticktok (ticktok conf)]
     (ticktok req))))


(facts :f "about ticktok"
       (with-state-changes [(before :contents (start-ticktok))
                            (after :contents (stop-ticktok))]

         (facts :f "when ticktok failed to fetch clock"

                (facts "when ticktok server failed to respond"
                       (with-state-changes [(before :contents (stub-ticktok-returned-bad-request))]

                         (fact "should fail if ticktok server not found"
                               (register-clock)) => (throws RuntimeException #"Failed to fetch clock" #(= (:status (ex-data %)) 400))

                         (fact "should ask from ticktok server clock"
                               (stub-ticktok-incoming-request) => (contains {:name (:name clock-request)
                                                                             :schedule (:schedule clock-request)})))))

       (facts :f "when clock is successfully sent"

                (with-state-changes [(before :facts (stub-ticktok-respond-with-clock-and-schedule-ticks clock))]
                  (let [ch (chan 1)
                        clock-request (make-clock-request #(put! ch "got tick"))
                        clock (stub/make-clock-from clock-request)
                        is-inovked #(let [m (<!! ch)]
                                      (close! ch)
                                      m)]

                    (fact "should invoke callback upon tick"
                          (register-clock clock-request) => true
                          (stub/send-tick) => true
                          (is-inovked) => truthy
                          ))))))

(facts "about clock validity"
       (tabular
        (fact "should return fail for invalid clock request"
              (register-clock ?host ?clock)) => (throws RuntimeException #"Invalid input")
        ?host ?clock
        host {:name "my.clock"}
        host {:name "my.clock" :schedule "Every.3.seconds"}
        host {:name "my.clock" :schedule "every.0.seconds"}
        "" {:name "my.clock" :schedule "every.3.seconds"}
        ))

(ns ticktok.core-test
  (:require [clojure.test :refer :all]
            [ticktok.core :refer :all]
            [ticktok.stub-ticktok :as stub]
            [midje.sweet :refer :all]
            [clojure.data.json :as json]
            [clojure.core.async :as async :refer [chan put! <!!]]))

(def host  "http://localhost:8080")

(def state (atom {:stub-ticktok nil}))

(def config {:host host :token "my.token"})

(defn start-ticktok []
  (swap! state assoc :stub-ticktok (stub/start)))

(defn stop-ticktok []
  (swap! state update :stub-ticktok stub/stop))

(defn stub-ticktok []
  (get @state :stub-ticktok))

(defn stub-respond-with [resp]
  (stub/respond-with (stub-ticktok) resp))

(defn make-clock-request
  ([]
   (make-clock-request #()))
  ([callback]
   {:name "myclock"
    :schedule "every.5.seconds"
    :callback callback}))

(def clock-request (make-clock-request))

(def clock (stub/make-clock-from clock-request))

(defn stub-ticktok-respond-with-clock []
  (println "stub-ticktok-respond-with-clock")
  (stub-respond-with clock)
  true)

(defn stub-ticktok-is-not-found []
  (println "stub-ticktok-is-not-found")
  (stub-respond-with {:status 404})
  true)

(defn stub-ticktok-respond-with-invalid-clock []
  (stub-respond-with (stub/make-clock-from {})))

(defn clock-from [clock-req]
  (select-keys clock-req [:name :schedule]))

(facts "about ticktok"
       (with-state-changes [(before :contents (start-ticktok))
                            (after :contents (stop-ticktok))]
         (facts "when ticktok server not found"
                (with-state-changes [(before :contents (stub-ticktok-is-not-found))]
                  (fact "should fail if ticktok server not found"
                        (ticktok config clock-request)) => (throws RuntimeException #"Failed to fetch clock")
                  (fact "should ask from ticktok server clock"
                        (:body (stub/incoming-request (stub-ticktok))) => (clock-from clock-request))
                  ))))

(comment (facts :unit "about clock validity"
                (tabular
                 (fact :unit "should return fail for invalid clock request"
                       (ticktok ?host ?clock)) => (throws RuntimeException #"")
                 ?host ?clock
                 "" {}
                 )))


(comment (fact  "should fail if failed to parse ticktok server response"
                (against-background (stub-ticktok-respond-with-invalid-clock) => true)
                (ticktok config clock-request)))

(comment (fact  "should fail if queue wasn't found"
                (stub-ticktok-respond-with-clock) => true
                (ticktok config clock-request)) => (throws RuntimeException #"Failed to find queue"))



(comment (let [ch (chan 1)
               clock-request (make-clock-request #(put! ch "got tick"))
               clock (stub/make-clock-from clock-request)
               is-inovked #(nil? (<!! ch))]
           (against-background [(before :facts (do
                                                 (stub/respond-with (stub-ticktok) clock)
                                                 (stub/schedule-ticks)))]
                               (fact "should invoke callback upon tick"
                                     (ticktok config clock-request) => (:body clock)
                                     (stub/send-tick) => true
                                     (is-inovked) => true
                                     ))))

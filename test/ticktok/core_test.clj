(ns ticktok.core-test
  (:require [clojure.test :refer :all]
            [ticktok.core :refer :all]
            [ticktok.stub-ticktok :as stub]
            [midje.sweet :refer :all]
            [clojure.data.json :as json]
            [clojure.core.async :as async :refer [chan put! <!!]])
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

(defn make-clock-request
  ([]
   (make-clock-request #()))
  ([callback]
   {:name "myclock"
    :schedule "every.5.seconds"
    :callback callback}))

(def clock-request (make-clock-request))

(def clock (stub/make-clock-from clock-request))

(defn clock-from [clock-req]
  (select-keys clock-req [:name :schedule]))

(facts :unit "about clock validity"
       (tabular
        (fact :unit "should return fail for invalid clock request"
              (ticktok ?host ?clock)) => (throws RuntimeException #"")
        ?host ?clock
        "" {}
        ))

(facts "about ticktok"
       (with-state-changes [(before :contents (start-ticktok))
                            (after :contents (stop-ticktok))]
         (against-background [(before :facts (stub-ticktok-is-not-found))]
                             (fact :slow "should fail if ticktok server not found"
                                   (ticktok config clock-request)) => (throws RuntimeException #"")
                             (fact "should call for ticktok server"
                                   (:body (stub/incoming-request (stub-ticktok))) => (clock-from clock-request)))
         (let [clock-details (json/read-str (:body clock) :key-fn keyword)]
           (against-background [(before :facts (do
                                                 (stub/respond-with (stub-ticktok) clock)
                                                 (stub/schedule-ticks)))]
                               (fact "should return clock details"
                                     (ticktok config clock-request) => clock-details)))
         ))





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

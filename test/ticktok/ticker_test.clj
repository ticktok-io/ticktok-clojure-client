(ns ticktok.ticker-test
  (:require [clojure.test :refer :all]
            [ticktok.ticker :as ticker]
            [ticktok.stub-ticktok :as stub]
            [midje.sweet :refer :all]
            [ticktok.domain :as dom]))

(def host  "http://localhost:8080")

(def config {:host host :token "my.token"})

(def state (atom {:stub-ticktok nil}))

(def clock-request {:name "my.clock" :schedule "every.5.seconds"})

(def clock (stub/make-clocks-from clock-request))

(defn start-ticktok []
  (swap! state assoc :stub-ticktok (stub/start!)))

(defn stop-ticktok []
  (swap! state update :stub-ticktok stub/stop!))

(defn stub-ticktok []
  (get @state :stub-ticktok))

(defn ticktok-respond-with-bad-request []
  (stub/respond-with (stub-ticktok) {:status 400})
  true)

(defn ticktok-respond-with-clock-but-failed-to-tick []
  (stub/respond-with (stub-ticktok) clock)
  true)

(facts :s "about tick on clock"

       (with-state-changes [(before :contents (start-ticktok))
                            (after :contents (stop-ticktok))]

         (facts "when failed to tick on clock"

                (with-state-changes [(before :contents (ticktok-respond-with-bad-request))]

                  (fact "should fail when failed to fetch clock"
                        (ticker/tick config clock-request)) => (throws RuntimeException #"Failed to fetch clock" #(contains? (ex-data %) :clock)))

                (with-state-changes [(before :contents (ticktok-respond-with-clock-but-failed-to-tick))]

                  (fact "should fail when failed to tick"
                        (ticker/tick config clock-request)) => (throws RuntimeException #"Failed to tick for clock" #(contains? (ex-data %) :clock))))))


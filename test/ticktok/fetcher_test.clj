(ns ticktok.fetcher-test
  (:require [clojure.test :refer :all]
            [ticktok.fetcher :refer [fetch-clock]]
            [ticktok.stub-ticktok :as stub]
            [midje.sweet :refer :all]
            [ticktok.domain :as dom]))

(def host  "http://localhost:8080")

(def state (atom {:stub-ticktok nil}))

(def clock-request {:name "my.clock" :schedule "every.5.seconds"})

(defn start-ticktok []
  (swap! state assoc :stub-ticktok (stub/start!)))

(defn stop-ticktok []
  (swap! state update :stub-ticktok stub/stop!))

(defn stub-ticktok []
  (get @state :stub-ticktok))

(defn stub-respond-with [resp]
  (stub/respond-with (stub-ticktok) resp))

(defn stub-ticktok-respond-with-invalid-clock []
  (println "stub-ticktok-respond-with-invalid-clock")
  (stub-respond-with (stub/make-clock-from {}))
  true)

(defn stub-ticktok-respond-with-clock []
  (println "stub-ticktok-respond-with-clock")
  (stub-respond-with (stub/make-clock-from clock-request))
  true)

(defn fetch []
  (fetch-clock host clock-request))

(facts :f "about fetching a clock"
       (with-state-changes [(before :contents (start-ticktok))
                            (after :contents (stop-ticktok))]

         (facts "when failed to fetch clock"

                (with-state-changes [(before :contents (stub-ticktok-respond-with-invalid-clock))]
                  (fact "should fail if ticktok server respond with invalid clock"
                        (fetch)) => (throws RuntimeException #"Failed to parse clock" #(contains? (ex-data %) :clock))))

         (facts "when ticktok respond with valid clock"

                (with-state-changes [(before :contents (stub-ticktok-respond-with-clock))]
                  (fact "should return clock details"
                          (fetch) => (contains {:channel (contains
                                                          {:queue string?
                                                           :uri string?})
                                                :name (:name clock-request)}))))))

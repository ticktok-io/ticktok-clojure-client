(ns ticktok.fetcher-test
  (:require [clojure.test :refer :all]
            [ticktok.fetcher :refer [fetch-clock]]
            [ticktok.stub-ticktok :as stub]
            [midje.sweet :refer :all]
            [ticktok.domain :as dom]))

(def host  "http://localhost:8080")

(def config {:host host :token "my.token"})

(def attempts 3)

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

(defn ticktok-respond-with-invalid-clock []
  (println "stub-ticktok-respond-with-invalid-clock")
  (stub-respond-with (stub/make-clock-from {}))
  true)

(defn ticktok-respond-with-clock []
  (println "stub-ticktok-respond-with-clock")
  (stub-respond-with (stub/make-clock-from clock-request))
  true)

(defn ticktok-finally-respond-with-clock [attempts]
  (stub/fail-for (stub-ticktok) attempts)
  (ticktok-respond-with-clock)
  true)

(defn fetch
  ([]
   (fetch 0))
  ([n]
   (fetch-clock config clock-request n)))

(facts  "about fetching a clock"

        (with-state-changes [(before :contents (start-ticktok))
                             (after :contents (stop-ticktok))]

          (facts  "when failed to fetch clock"

                  (with-state-changes [(before :contents (ticktok-respond-with-invalid-clock))]

                    (fact "should fail if ticktok server respond with invalid clock"
                          (fetch)) => (throws RuntimeException #"Failed to parse clock" #(contains? (ex-data %) :clock))))

          (facts "when ticktok respond with valid clock"

                 (with-state-changes [(before :contents (ticktok-respond-with-clock))]

                   (fact  "should return clock details"
                          (fetch) => (contains {:channel (contains
                                                          {:details (contains {:queue string?
                                                                               :uri string?})})
                                                :name (:name clock-request)})))

                 (with-state-changes [(before :contents (ticktok-finally-respond-with-clock attempts))]

                   (fact "should return clock details after retry"
                         (fetch attempts) => truthy)))))

# Ticktok.io Clojure Client

## Description
This is a Clojure client for [Ticktok.io](https://ticktok.io). It allows to easily creating new clocks and invoke actions on ticks.

## Installation

Ticktok artifacts are [published to Clojars](https://clojars.org/org.clojars.ticktok/ticktok).
Latest release via [Leiningen](https://leiningen.org/):

> [![Clojars Project](https://img.shields.io/clojars/v/org.clojars.ticktok/ticktok.svg)](https://clojars.org/org.clojars.ticktok/ticktok)


### Examples
### Register an action for a clock
Ticktok assumes default configuration when not provided, as documented in [Ticktok docs](https://ticktok.io/docs).
```clojure

(require '[ticktok.core :as tk])

(def config {:host "http://localhost:8080"
                     :token "ticktok-zY3wpR"})

(def ticktok (tk/ticktok :start config))

(ticktok :schedule {:name "hurry.up"
                    :schedule "every.3.seconds"
                    :callback #(println "First clockgot a tick!")})

(tk/ticktok :schedule config {:name "ease.in"
                              :schedule "every.1.hours"
                              :callback #(println "Second clock got a tick!")})

(ticktok :stop)

```

In the example above we registred two clocks in two supported ways: the first one is by calling ```(ticktok :start config)``` which returns us a function the waits to schedule clocks. In the second way, we call directly to ```(ticktok :schedule)``` with both the clock and the desired configuration.

## Community
Have some questions/ideas? chat with us on [Slack](https://join.slack.com/t/ticktokio/shared_invite/enQtNTE0MzExNTY5MjIzLThjNDU3NjIzYzQxZTY0YTM5ODE2OWFmMWU3YmQ1ZTViNDVmYjZkNWUzMWU5NWU0YmU5NWYxMWMxZjlmNGQ1Y2U)

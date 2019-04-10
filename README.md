# Ticktok.io Clojure Client
[![Clojars Project](https://img.shields.io/clojars/v/ticktok.svg)](https://clojars.org/ticktok)

## Description
This is a Clojure client for [Ticktok.io](https://ticktok.io). It allows you to easily creating new clocks and invoke actions on ticks.

## Installation

Ticktok artifacts are [released to Clojars](https://clojars.org/ticktok).

* [Leiningen](https://leiningen.org/):
```clojure
[ticktok "1.0.1"]
```

* [Maven](http://maven.apache.org/):
```pom
<dependency>
  <groupId>ticktok</groupId>
  <artifactId>ticktok</artifactId>
  <version>1.0.1</version>
</dependency>
```

* [Gradle](https://gradle.org/):
```
compile 'ticktok:ticktok:1.0.1'
```

## Quick Start

Ticktok assumes default configuration when not provided, as documented in [Ticktok docs](https://ticktok.io/docs).

### Register an action for a clock

```clojure

(require '[ticktok.core :as tk])

(def config {:host "http://localhost:8080"
             :token "ticktok-zY3wpR"})

(def ticktok (tk/ticktok :start config))

(ticktok :schedule {:name "hurry.up"
                    :schedule "every.3.seconds"
                    :callback #(println "First clock got a tick!")})

(tk/ticktok :schedule config {:name "ease.in"
                              :schedule "every.1.hours"
                              :callback #(println "Second clock got a tick!")})

(ticktok :close)

```

In the example above we registred two clocks in different supported ways: the first one is by calling `(ticktok :start config)` which returns us a function the waits to schedule clocks associated to the given `config`. For the second clock, we call directly to `(ticktok :schedule config clock)` with both the clock and the desired configuration. Behind the scenes, both ways are effectively the same.

To stop listening for new ticks, call ticktok with `:close`.

## Community
Have some questions/ideas? chat with us on [Slack](https://join.slack.com/t/ticktokio/shared_invite/enQtNTE0MzExNTY5MjIzLThjNDU3NjIzYzQxZTY0YTM5ODE2OWFmMWU3YmQ1ZTViNDVmYjZkNWUzMWU5NWU0YmU5NWYxMWMxZjlmNGQ1Y2U)

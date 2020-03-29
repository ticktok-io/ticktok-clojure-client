# Ticktok.io Clojure Client
[![Clojars Project](https://img.shields.io/clojars/v/ticktok.svg)](https://clojars.org/ticktok)

## Description
This is the official Clojure client for [Ticktok.io](https://ticktok.io). It allows you to easily create new clocks and invoke actions on ticks.

## Installation

Ticktok artifacts are [released to Clojars](https://clojars.org/ticktok).

* [Leiningen](https://leiningen.org/):
```clojure
[ticktok "1.0.10"]
```

* [Maven](http://maven.apache.org/):
```pom
<dependency>
  <groupId>ticktok</groupId>
  <artifactId>ticktok</artifactId>
  <version>1.0.10</version>
</dependency>
```

* [Gradle](https://gradle.org/):
```
compile 'ticktok:ticktok:1.0.10'
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

(tk/ticktok :close)

```

In the example above we registred two clocks in different supported ways:

* By calling `(ticktok :start config)` - returns us a function that waits to schedule clocks associated to the given `config`.
* By calling `(ticktok :schedule config clock)` - with both the clock and the desired configuration.

Behind the scenes, both ways are effectively the same.

To stop listening for new ticks, call ticktok with `:close`.

### Replace callback of a given clock

It's now (1.0.6+) possible to swap callback for a registered clock. When calling `(ticktok :schedule)` with the same clock but with different callback, ticktok swaps the consuming callback:

```clojure

(require '[ticktok.core :as tk])

(tk/ticktok :schedule {:name "hurry.up"
                      :schedule "every.3.seconds"
                      :callback #(println "First callback invoked!")})

(tk/ticktok :schedule {:name "hurry.up"
                      :schedule "every.3.seconds"
                      :callback #(println "Second callback invoked!")})

```

## Community
Have some questions or ideas? chat with us on [Slack](https://join.slack.com/t/ticktokio/shared_invite/enQtNTE0MzExNTY5MjIzLThjNDU3NjIzYzQxZTY0YTM5ODE2OWFmMWU3YmQ1ZTViNDVmYjZkNWUzMWU5NWU0YmU5NWYxMWMxZjlmNGQ1Y2U)

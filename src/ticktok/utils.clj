(ns ticktok.utils
  (:require [clojure.pprint :as pp]
            [ticktok.domain :as dom]
            [perseverance.core :as p]))

(def default-delay 100)

(defn pretty [obj]
  (pp/pprint obj))

(defmacro safe [body]
  `(try
     ~body
     (catch Exception e#
       (str "caught exception: " (.getMessage e#)))))

(defmacro fail-with [msg details]
  `(throw (ex-info ~msg ~details)))

(defmacro fail-with-inner-ex [e]
  `(let [src-ex# (:e (ex-data ~e))
         src-ex# (Throwable->map src-ex#)
         src-ex# (first (:via src-ex#)) ]
     (fail-with (:message src-ex#) (:data src-ex#))))

(defmacro retry [f attempts]
  `(try
    (p/retry {:strategy (p/constant-retry-strategy ~default-delay ~attempts)}
      (p/retriable {:catch [RuntimeException]}
        ~f))
    (catch Exception e#
      (fail-with-inner-ex e#))))

(ns ticktok.utils
  (:require [clojure.pprint :as pp]
            [ticktok.domain :as dom]
            [perseverance.core :as p]))

(def retry-defaults {:max-delay 10000
                     :initial-delay 2000
                     :max-count 4})

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
         src-ex#      (Throwable->map src-ex#)
         src-ex# (first (:via src-ex#)) ]
     (fail-with (:message src-ex#) (:data src-ex#))))

(defmacro retry [f attempts]
  `(try
     (p/retry {:strategy (p/progressive-retry-strategy
                          :max-count (:max-count retry-defaults)
                          :initial-delay (:initial-delay retry-defaults)
                          :max-delay (:max-delay retry-defaults))}
       (p/retriable {:catch [RuntimeException]}
         ~f))
    (catch Exception e#
      (fail-with-inner-ex e#))))

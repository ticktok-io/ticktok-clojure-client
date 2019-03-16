(ns ticktok.utils
  (:require [clojure.pprint :as pp]
            ))

(defn pretty [obj]
  (pp/pprint obj))

(defn fail-with
  ([msg]
   (fail-with msg {}))
  ([msg details]
   (println "debug: error")
   (pretty msg)
   (pretty details)
   (throw (ex-info msg details))))

(defmacro safe [body]
  `(try
     ~body
     (catch Exception e#
       (str "caught exception: " (.getMessage e#)))))

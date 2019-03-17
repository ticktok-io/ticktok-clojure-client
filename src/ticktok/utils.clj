(ns ticktok.utils
  (:require [clojure.pprint :as pp]
            ))

(defn pretty [obj]
  (pp/pprint obj))

(defmacro safe [body]
  `(try
     ~body
     (catch Exception e#
       (str "caught exception: " (.getMessage e#)))))

(defn fail-with
  ([msg]
   (fail-with msg {}))
  ([msg details]
   (throw (ex-info msg details))))

(ns ticktok.utils
  (:require [clojure.pprint :as pp]
            [ticktok.domain :as dom]))

(defn pretty [obj]
  (pp/pprint obj))

(defmacro safe [body]
  `(try
     ~body
     (catch Exception e#
       (str "caught exception: " (.getMessage e#)))))

(defn fail-with
  ([e]
   (let [src-ex (:e (ex-data e))
         src-ex (Throwable->map src-ex)
         {:keys [message data]} (first (:via src-ex))]
   (fail-with message data)))
  ([msg details]
   (throw (ex-info msg details))))

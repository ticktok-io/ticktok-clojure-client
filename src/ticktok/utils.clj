(ns ticktok.utils
  (:require [clojure.pprint :as pp]
            [ticktok.domain :as dom]
            [clojure.spec.alpha :as s]))

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

(defn validate-input [type entity]
  (let [parsed (dom/conform {:type type :entity entity})]
    (if (= ::s/invalid parsed)
      (dom/invalid-input type entity)
      parsed)))

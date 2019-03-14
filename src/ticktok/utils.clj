(ns ticktok.utils)

(defn fail-with
  ([msg]
   (fail-with msg {}))
  ([msg details]
   (println "error:" msg ", details:" details)
   (throw (ex-info msg details))))

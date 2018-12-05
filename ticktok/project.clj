(defproject ticktok "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.9.0"]
                 [compojure "1.6.1"]
                 [http-kit "2.2.0"]
                 [hiccup "1.0.5"]
                 [cheshire "5.8.1"]
                 [ring "1.7.1"]
                 [org.clojure/data.json "0.2.6"]
                 [ring/ring-json "0.4.0"]
                 [org.clojure/core.async "0.4.490"]]
  :jvm-opts ["--add-modules" "java.xml.bind"]
  :main ^:skip-aot ticktok.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})

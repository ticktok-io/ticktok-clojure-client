(defproject ticktok "0.1.0-SNAPSHOT"
  :description "ticktok clojure client"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [compojure "1.6.1"]
                 [http-kit "2.2.0"]
                 [hiccup "1.0.5"]
                 [cheshire "5.8.1"]
                 [ring "1.7.1"]
                 [org.clojure/data.json "0.2.6"]
                 [ring/ring-json "0.4.0"]
                 [org.clojure/core.async "0.4.490"]
                 [com.novemberain/langohr "5.0.0"]]
  :jvm-opts ~(let [version     (System/getProperty "java.version")
                   [major _ _] (clojure.string/split version #"\.")]
               (if (>= (java.lang.Integer/parseInt major) 9)
                 ["--add-modules" "java.xml.bind"]
                 []))
  :main ^:skip-aot ticktok.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}}
  :deploy-repositories   :deploy-repositories [["releases"
                                                {:url
                                                 "https://api.bintray.com/content/ticktok-io/maven/ticktok-clojure-client/"
                                                 :sign-releases false
                                                 :username :env/bintray_username
                                                 :password :env/bintray_api_key}]
                                               ["snapshots"
                                                {:url "https://api.bintray.com/content/ticktok-io/maven/ticktok-clojure-client/"
                                                 :sign-releases false
                                                 :username :env/bintray_username
                                                 :password :env/bintray_api_key}]]


  [["releases"
                         {:url
                         "https://dl.bintray.com/ticktok-io/maven/ticktok-clojure-client/;publish=1"
                          :sign-releases false
                          :username :env/bintray_username
                          :password :env/bintray_api_key}]
                        ["snapshots"
                         {:url "https://dl.bintray.com/ticktok-io/maven/ticktok-clojure-client/;publish=1"
                          :sign-releases false
                          :username :env/bintray_username
                          :password :env/bintray_api_key}]])

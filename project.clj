(defproject ticktok "0.1.0-SNAPSHOT"
  :description "ticktok clojure client"
  :url "https://ticktok.io"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.10.0"]
                 [http-kit "2.2.0"]
                 [org.clojure/data.json "0.2.6"]
                 [com.novemberain/langohr "5.0.0"]]
  :jvm-opts ~(let [version     (System/getProperty "java.version")
                   [major _ _] (clojure.string/split version #"\.")]
               (if (>= (java.lang.Integer/parseInt major) 9)
                 ["--add-modules" "java.xml.bind"]
                 []))
  :main ^:skip-aot ticktok.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}
             :dev {:dependencies [[org.clojure/clojure "1.8.0"]
                                  [compojure "1.6.1"]
                                  [ring "1.7.1"]
                                  [org.clojure/core.async "0.4.490"]
                                  [ring/ring-json "0.4.0"]
                                  [com.novemberain/langohr "5.0.0"]]}}

  :release-tasks [["vcs" "assert-committed"]
                  ["change" "version"
                   "leiningen.release/bump-version" "release"]
                  ["vcs" "commit"]
                  ["vcs" "tag" "--no-sign"]
                  ["deploy"]
                  ["change" "version" "leiningen.release/bump-version"]
                  ["vcs" "commit"]
                  ["vcs" "push"]]

  :deploy-repositories [["releases"
                         {:url "https://api.bintray.com/content/ticktok-io/maven/ticktok-clojure-client/"
                          :sign-releases false
                          :username :env/bintray_username
                          :password :env/bintray_api_key}]
                        ["snapshots"
                         {:url "https://api.bintray.com/content/ticktok-io/maven/ticktok-clojure-client/"
                          :sign-releases false
                          :username :env/bintray_username
                          :password :env/bintray_api_key}]])

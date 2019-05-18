(defproject ticktok/ticktok "1.0.6-SNAPSHOT"
  :description "ticktok clojure client"
  :url "https://ticktok.io"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.10.0"]
                 [http-kit "2.2.0"]
                 [org.clojure/data.json "0.2.6"]
                 [com.novemberain/langohr "5.0.0"]
                 [com.grammarly/perseverance "0.1.3"]
                 [overtone/at-at "1.2.0"]]
  :jvm-opts ~(let [version     (System/getProperty "java.version")
                   [major _ _] (clojure.string/split version #"\.")]
               (if (>= (java.lang.Integer/parseInt major) 9)
                 ["--add-modules" "java.xml.bind"]
                 []))
  :main ^:skip-aot ticktok.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}
             :dev {:dependencies [[compojure "1.6.1"]
                                  [ring "1.7.1"]
                                  [org.clojure/core.async "0.4.490"]
                                  [ring/ring-json "0.4.0"]
                                  [com.novemberain/langohr "5.0.0"]
                                  [midje "1.9.6"]]
                   :plugins [[lein-midje "3.2.1"]]}}

  :release-tasks [["vcs" "assert-committed"]
                  ["change" "version"
                   "leiningen.release/bump-version" "release"]
                  ["vcs" "commit"]
                  ["vcs" "tag" "--no-sign"]
                  ["deploy"]
                  ["change" "version" "leiningen.release/bump-version"]
                  ["vcs" "commit"]
                  ["vcs" "push"]]

  :repositories [["releases" {:url "https://clojars.org/repo"
                              :sign-releases false
                              :username :env/clojars_username
                              :password :env/clojars_password}]
                 ["snapshots" {:url "https://clojars.org/repo"
                               :sign-releases false
                               :username :env/clojars_username
                               :password :env/clojars_password}]])

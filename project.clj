(defproject clj-chorebot "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url  "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.9.0"]
                 [org.clojure/java.jdbc "0.7.3"]
                 [java-jdbc/dsl "0.1.3"]
                 [org.postgresql/postgresql "42.1.4"]
                 [slack-rtm "0.1.6"]
                 [org.julienxx/clj-slack "0.5.5"]
                 [org.clojure/core.match "0.3.0-alpha5"]
                 [org.clojure/spec.alpha "0.1.143"]
                 [sqlingvo "0.9.16"]
                 [environ "1.1.0"]]
  :main clj-chorebot.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})

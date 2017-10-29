(defproject clj-chorebot "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [slack-rtm "0.1.6"]
                 [org.clojure/java.jdbc "0.7.3"]
                 [org.postgresql/postgresql "42.1.4"]]
  :main ^:skip-aot clj-chorebot.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})

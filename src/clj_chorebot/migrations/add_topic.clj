(ns clj-chorebot.migrations.add-topic
  (:require [clojure.java.jdbc :as sql]
            [clj-chorebot.config :as config]
            [clj-chorebot.commands.commands :as commands]))

(def migration-name "add-topic")

(defn migrated? []
  (-> (sql/query config/db-url
                 [(str "select count(*) from migrations "
                       "where name='" migration-name "'")])
      first :count pos?))

(defn migrate []
  (when (not (migrated?))
    (print "Setting topic...") (flush)
    (commands/set-topic)
    (sql/insert! config/db-url :migrations {:name migration-name})
    (println " done")))

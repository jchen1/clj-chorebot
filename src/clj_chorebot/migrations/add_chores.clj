(ns clj-chorebot.migrations.add-chores
  (:require [clojure.java.jdbc :as sql]
            [clj-chorebot.config :as config]))

(def migration_name "add-chores")

(defn migrated? []
  (-> (sql/query config/db-url
                 [(str "select count(*) from migrations "
                       "where name='" migration_name "'")])
      first :count pos?))

(defn migrate []
  (when (not (migrated?))
    (print "Adding dishes/trash chores...") (flush)
    (sql/with-db-transaction [t-con config/db-url]
      (sql/insert! t-con :chores {:name "dishes" :description "wash and put away the dishes."})
      (sql/insert! t-con :chores {:name "trash" :description "take out the trash and boxes."})
      (sql/insert! t-con :migrations {:name migration_name}))
    (println " done")))

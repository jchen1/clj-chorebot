(ns clj-chorebot.migrations.add_chores
  (:require [clojure.java.jdbc :as sql]
            [clj-chorebot.config :as config]))

(def migration_name "add_chores")

(defn migrated? []
  (-> (sql/query config/db_url
                 [(str "select count(*) from migrations "
                       "where name='" migration_name "'")])
      first :count pos?))

(defn migrate []
  (when (not (migrated?))
    (print "Adding dishes/trash chores...") (flush)
    (sql/with-db-transaction [t-con config/db_url]
      (sql/insert! t-con :chores {:name "dishes" :description "Wash and put away the dishes"})
      (sql/insert! t-con :chores {:name "trash" :description "Take out the trash and boxes"})
      (sql/insert! t-con :migrations {:name migration_name}))
    (println " done")))

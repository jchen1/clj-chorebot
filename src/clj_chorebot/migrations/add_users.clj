(ns clj-chorebot.migrations.add_users
    (:require [clojure.java.jdbc :as sql]
              [clj-chorebot.config :as config]))

  (def migration_name "add_users")

  (defn migrated? []
    (-> (sql/query config/db_url
                   [(str "select count(*) from migrations "
                         "where name='" migration_name "'")])
        first :count pos?))

  (defn migrate []
    (when (not (migrated?))
      (print "Adding admin user (jeff)...") (flush)
      (sql/with-db-transaction [t-con config/db_url]
        (sql/insert! t-con :users {:chore_order 0 :is_admin true :slack_dm_channel "D3Y428C5S" :slack_handle "jeff" :slack_id "U3VS17UQZ"})
        (sql/insert! t-con :migrations {:name migration_name}))
      (println " done")))

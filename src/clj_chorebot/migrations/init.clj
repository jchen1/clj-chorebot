(ns clj-chorebot.migrations.init
    (:require [clojure.java.jdbc :as sql]
              [clj-chorebot.config :as config]))

  (defn migrated? []
    (-> (sql/query config/db_url
                   [(str "select count(*) from information_schema.tables "
                         "where table_name='migrations'")])
        first :count pos?))

  (defn migrate []
    (when (not (migrated?))
      (print "Creating database structure...") (flush)
      (sql/db-do-commands config/db_url
                          [(sql/create-table-ddl
                            :users
                            [[:id :serial "PRIMARY KEY"]
                             [:chore_order :integer "NOT NULL" "UNIQUE"]
                             [:is_admin :boolean "NOT NULL" "DEFAULT FALSE"]
                             [:slack_dm_channel :varchar "NOT NULL" "UNIQUE"]
                             [:slack_handle :varchar "NOT NULL" "UNIQUE"]
                             [:slack_id :varchar "NOT NULL" "UNIQUE"]]
                            )
                           (sql/create-table-ddl
                            :chores
                            [[:id :serial "PRIMARY KEY"]
                             [:name :varchar "NOT NULL" "UNIQUE"]
                             [:description :varchar "NOT NULL"]]
                            )
                           (sql/create-table-ddl
                            :chorelogs
                            [[:id :serial "PRIMARY KEY"]
                             [:chore_id :integer "NOT NULL" "REFERENCES chores"]
                             [:user_id :integer "NOT NULL" "REFERENCES users"]
                             [:completed_at :timestamp "NOT NULL" "DEFAULT CURRENT_TIMESTAMP"]]
                            )
                           (sql/create-table-ddl
                            :migrations
                            [[:id :serial "PRIMARY KEY"]
                             [:name :varchar "NOT NULL"]
                             [:completed_at :timestamp "NOT NULL" "DEFAULT CURRENT_TIMESTAMP"]])
                           ])
      (sql/insert! config/db_url :migrations {:name "init"})
      (println " done")))

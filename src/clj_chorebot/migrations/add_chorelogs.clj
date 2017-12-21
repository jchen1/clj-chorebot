(ns clj-chorebot.migrations.add-chorelogs
  (:require [clojure.java.jdbc :as jdbc]
            [clj-chorebot.config :as config]
            [clj-chorebot.util.sql :as sql]))

(def migration-name "add-chorelogs")

(defn migrated? []
  (-> (jdbc/query config/db-url
                  (sql/select [`(count :*)]
                              (sql/from :migrations)
                              (sql/where `(= :name ~migration-name))))
      first :count pos?))

(defn migrate []
  (when (not (migrated?))
    (print "Adding initial chorelogs...") (flush)
    (jdbc/with-db-transaction [t-con config/db-url]
      (let [chores (jdbc/query t-con (sql/select [:id] :chores))
            users (jdbc/query t-con (sql/select [:id] :users))
            chorelogs (flatten (map #(map (fn [{user-id :id}] {:chore_id (:id %) :user_id user-id}) users) chores))]
        (run! #(jdbc/insert! t-con :chorelogs %) chorelogs))
      (jdbc/insert! t-con :migrations {:name migration-name}))
    (println " done")))

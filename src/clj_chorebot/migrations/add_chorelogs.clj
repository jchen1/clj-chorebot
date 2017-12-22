(ns clj-chorebot.migrations.add-chorelogs
  (:require [clojure.java.jdbc :as jdbc]
            [clj-chorebot.config :as config]
            [clj-chorebot.util.sql :as sql]))

(def migration-name "add-chorelogs")

(defn migrated? []
  (-> (sql/query config/db-url
                  (sql/select [`(count :*)]
                              (sql/from :migrations)
                              (sql/where `(= :name ~migration-name))))
      first :count pos?))

(defn migrate []
  (when (not (migrated?))
    (print "Adding initial chorelogs...") (flush)
    (jdbc/with-db-transaction [t-con config/db-url]
      (let [chores (sql/query t-con (sql/select [:id] (sql/from :chores)))
            users (sql/query t-con (sql/select [:id] (sql/from :users)))
            chorelogs (flatten (map #(map (fn [{user-id :id}] {:chore-id (:id %) :user-id user-id}) users) chores))]
        (run! #(sql/insert! t-con :chorelogs %) chorelogs))
      (sql/insert! t-con :migrations {:name migration-name}))
    (println " done")))

(ns clj-chorebot.services.chore-service
  (:require [clj-chorebot.config :as config]
            [clj-chorebot.models.chore :as chore]
            [clj-chorebot.models.user :as user]
            [clj-chorebot.models.chorelog :as chorelog]
            [clojure.java.jdbc :as jdbc]
            [clj-chorebot.util.sql :as sql]))


(defn remove
  [chore-name]
  (jdbc/with-db-transaction
    [t-con config/db-url]
    (when-let [{:keys [id] :as chore} (first (sql/find-by-keys t-con :chores {:name chore-name}))]
      (jdbc/execute! t-con (chorelog/get-remove-sql id))
      (jdbc/execute! t-con (chore/get-remove-sql chore-name))
      chore)))

(defn add
  [chore-name description]
  (jdbc/with-db-transaction
    [t-con config/db-url]
    (let [[{:keys [id] :as chore}] (sql/insert! t-con :chores {:name chore-name :description description})]
      (sql/insert! t-con :chorelogs {:user-id (:id (user/get-next-user 0)) :chore-id id}))))
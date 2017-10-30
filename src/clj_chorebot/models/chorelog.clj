(ns clj-chorebot.models.chorelog
  (:require [clj-chorebot.config :as config]
            [clj-chorebot.models.user :as user]
            [clj-chorebot.models.chore :as chore]
            [clj-chorebot.util.sql-utils :as sql-utils]
            [clojure.java.jdbc :as jdbc]
            [java-jdbc.sql :as sql]))

(defn complete_chore
  "complete a chore. returns next user's slack handle"
  [chore_name slack_id]
  (let [chore_obj (chore/get_by_name chore_name)
        user_obj (user/get-by-slack-id slack_id)]
    (do
      (jdbc/insert! config/db-url :chorelogs {:user_id (:id user_obj) :chore_id (:id chore_obj)})
      (:slack_handle (user/get-next-user (:chore_order user_obj)))
      )))

(defn get_last
  "gets last completed by + ts + description"
  [chore_name]
  (first (jdbc/query config/db-url (sql-utils/limit 1 (sql/select [:cl.completed_at :u.* :c.description] {:chorelogs :cl}
    (sql/join {:chores :c} {:cl.chore_id :c.id})
    (sql/join {:users :u} {:cl.user_id :u.id})
    (sql/where {:name chore_name})
    (sql/order-by {:completed_at :desc}))))))

(defn get_next
  "gets next user"
  [chore_name]
  (user/get-next-user (:chore_order (:user (get_last chore_name)))))

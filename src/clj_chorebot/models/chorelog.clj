(ns clj-chorebot.models.chorelog
  (:require [clj-chorebot.config :as config]
            [clj-chorebot.models.user :as user]
            [clj-chorebot.models.chore :as chore]
            [clj-chorebot.util.sql :as sql]
            [clojure.java.jdbc :as jdbc]))

(defn complete_chore
  "complete a chore. returns next user's slack handle"
  [chore-name slack_id]
  (let [chore_obj (chore/get-by-name chore-name)
        user_obj (user/get-by-slack-id slack_id)]
    (do
      (jdbc/insert! config/db-url :chorelogs {:user_id (:id user_obj) :chore_id (:id chore_obj)})
      (:slack_handle (user/get-next-user (:chore_order user_obj))))))

(defn get-last-all []
  (jdbc/query config/db-url
              (sql/select [:chorelogs.completed-at :users.* :chores.description :chores.name]
                          (sql/from :chorelogs)
                          (sql/join :chores.id :chorelogs.chore-id)
                          (sql/join :users.id :chorelogs.user-id)
                          (sql/where `(= :chorelogs.completed-at ~(sql/inner-select ['(max :completed-at)]
                                                                (sql/from (sql/as :chorelogs :cl))
                                                                (sql/where `(= :chorelogs.id :cl.id)))))
                          (sql/order-by (sql/desc :completed-at)))))

(defn get-last
  "gets last completed by + ts + description"
  [chore-name]
  (jdbc/query config/db-url (sql/select [:chorelogs.completed-at :users.* :chores.description :chores.name]
                                        (sql/from :chorelogs)
                                        (sql/join :chores.id :chorelogs.chore-id)
                                        (sql/join :users.id :chorelogs.user-id)
                                        (sql/where `(= :name ~chore-name))
                                        (sql/order-by (sql/desc :completed-at))
                                        (sql/limit 1))))

(defn get-next
  "gets next user"
  [chore-name]
  (user/get-next-user (:chore_order (:user (get-last chore-name)))))

(defn get-remove-sql
  [chore-id]
  (sql/delete :chorelogs (sql/where `(= :chore-id ~chore-id))))
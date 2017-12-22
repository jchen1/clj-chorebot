(ns clj-chorebot.models.chorelog
  (:require [clj-chorebot.config :as config]
            [clj-chorebot.models.user :as user]
            [clj-chorebot.models.chore :as chore]
            [clj-chorebot.util.sql :as sql]
            [clojure.java.jdbc :as jdbc]))

(defn complete-chore
  "complete a chore. returns next user's slack handle"
  [chore-name slack-id]
  (let [chore-obj (chore/get-by-name chore-name)
        user-obj (user/get-by-slack-id slack-id)]
    (do
      (sql/insert! config/db-url :chorelogs {:user-id (:id user-obj) :chore-id (:id chore-obj)})
      (:slack-handle (user/get-next-user (:chore-order user-obj))))))

(defn get-last-all []
  (sql/query config/db-url
              (sql/select [:chorelogs.completed-at :users.* :chores.description :chores.name]
                          (sql/from :chorelogs)
                          (sql/join :chores.id :chorelogs.chore-id)
                          (sql/join :users.id :chorelogs.user-id)
                          (sql/where `(= :chorelogs.completed-at ~(sql/inner-select ['(max :completed-at)]
                                                                (sql/from (sql/as :chorelogs :cl))
                                                                (sql/where `(= :chorelogs.chore-id :cl.chore-id)))))
                          (sql/order-by (sql/desc :completed-at)))))

(defn get-last
  "gets last completed by + ts + description"
  [chore-name]
  (sql/query config/db-url (sql/select [:chorelogs.completed-at :users.* :chores.description :chores.name]
                                        (sql/from :chorelogs)
                                        (sql/join :chores.id :chorelogs.chore-id)
                                        (sql/join :users.id :chorelogs.user-id)
                                        (sql/where `(= :name ~chore-name))
                                        (sql/order-by (sql/desc :completed-at))
                                        (sql/limit 1))))

(defn get-next
  "gets next user"
  [chore-name]
  (user/get-next-user (:chore-order (:user (get-last chore-name)))))

(defn get-remove-sql
  [chore-id]
  (sql/delete :chorelogs (sql/where `(= :chore-id ~chore-id))))
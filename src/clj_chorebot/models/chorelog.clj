(ns clj-chorebot.models.chorelog
  (:require [clj-chorebot.config :as config]
            [clj-chorebot.models.user :as user]
            [clj-chorebot.models.chore :as chore]
            [clj-chorebot.util.sql :as sql]
            [clojure.java.jdbc :as jdbc]))

(defn complete_chore
  "complete a chore. returns next user's slack handle"
  [chore_name slack_id]
  (let [chore_obj (chore/get_by_name chore_name)
        user_obj (user/get-by-slack-id slack_id)]
    (do
      (jdbc/insert! config/db-url :chorelogs {:user_id (:id user_obj) :chore_id (:id chore_obj)})
      (:slack_handle (user/get-next-user (:chore_order user_obj)))
      )))

(defn get-last-all []
  (jdbc/query config/db-url (sql/select [:*] {:chorelogs :c}
                                        (sql/where {:completed_at ^:subquery (sql/select "max(completed_at)" {:chorelogs :cl}
                                                                              (sql/where {:cl.id "c.id"}))}))))

(defn get_last
  "gets last completed by + ts + description"
  [chore_name]
  (first (jdbc/query config/db-url (sql/limit 1 (sql/select [:cl.completed_at :u.* :c.description] {:chorelogs :cl}
                                                                  (sql/join {:chores :c} {:cl.chore_id :c.id})
                                                                  (sql/join {:users :u} {:cl.user_id :u.id})
                                                                  (sql/where {:name chore_name})
                                                                  (sql/order-by {:completed_at :desc}))))))

(defn get_next
  "gets next user"
  [chore_name]
  (user/get-next-user (:chore_order (:user (get_last chore_name)))))

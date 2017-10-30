(ns clj-chorebot.models.user
  (:require [clj-chorebot.config :as config]
            [clojure.java.jdbc :as jdbc]
            [java-jdbc.sql :as sql]))

(defn create
  "Creates a user"
  [user]
  (jdbc/with-db-transaction [t-con config/db-url]
    (let
      [max_order (or (:chore_order (first (jdbc/query t-con ["SELECT chore_order FROM users WHERE chore_order = (SELECT max(chore_order) FROM users)"]))) -1)]
      (jdbc/insert! t-con :users (assoc user :chore_order (+ 1 max_order))))))

(defn decrement_chore_orders_above_n
  [t-con n]
  (jdbc/execute! t-con ["UPDATE users SET chore_order = chore_order - 1 WHERE chore_order > ?" n]))

(defn remove
  "Removes a user, and decrements chore_order"
  [slack_handle]
  (jdbc/with-db-transaction [t-con config/db-url]
    (let [[user] (jdbc/find-by-keys t-con :users {:slack_handle slack_handle})]
      (do
        (jdbc/delete! t-con :users ["slack_handle=?" slack_handle])
        (if user
          (decrement_chore_orders_above_n t-con (get user :chore_order))
          (println str (slack_handle " is not a user")))))))

(defn set_admin
  [slack_handle is_admin]
  (jdbc/update! config/db-url :users {:is_admin is_admin} (sql/where {:slack_handle slack_handle})))

(defn get_by_username
  [slack_handle]
  (first (jdbc/find-by-keys config/db-url :users {:slack_handle slack_handle})))

(defn get_by_slack_id
  [slack_id]
  (first (jdbc/find-by-keys config/db-url :users {:slack_id slack_id})))

(defn get_next_user
  "gets next user in chore sequence"
  [chore_order]
  (jdbc/with-db-transaction [t-con config/db-url]
    (let [max_chore_order (:count (first (jdbc/query t-con (sql/select "count(*)" :users))))
          next_order (mod (+ 1 (or chore_order 0)) max_chore_order)]
        (first (jdbc/find-by-keys t-con :users {:chore_order next_order})))))

(ns clj-chorebot.models.user
  (:require [clj-chorebot.config :as config]
            [clojure.java.jdbc :as jdbc]
            [clj-chorebot.util.sql :as sql]))

(defn create
  "Creates a user"
  [user]
  (jdbc/with-db-transaction [t-con config/db-url]
                            (let
                              [max_order (or (:chore_order (first (jdbc/query t-con ["SELECT chore_order FROM users WHERE chore_order = (SELECT max(chore_order) FROM users)"]))) -1)]
                              (jdbc/insert! t-con :users (assoc user :chore_order (+ 1 max_order))))))

(defn decrement-chore-orders-above-n
  [t-con n]
  (jdbc/execute! t-con ["UPDATE users SET chore_order = chore_order - 1 WHERE chore_order > ?" n]))

(defn remove
  "Removes a user, and decrements chore_order"
  [slack-handle]
  (jdbc/with-db-transaction [t-con config/db-url]
                            (let [[user] (jdbc/find-by-keys t-con :users {:slack_handle slack-handle})]
                              (when user
                                (jdbc/delete! t-con :users ["slack_handle=?" slack-handle])
                                (decrement-chore-orders-above-n t-con (:chore_order user))
                                user))))

(defn set-admin
  [slack-handle is-admin]
  (< 0 (first (jdbc/execute! config/db-url
                             (sql/update :users {:is-admin is-admin}
                                         (sql/where `(= :slack-handle ~slack-handle)))))))

(defn get-by-username
  [slack-handle]
  (first (jdbc/find-by-keys config/db-url :users {:slack_handle slack-handle})))

(defn get-by-slack-id
  [slack-id]
  (first (jdbc/find-by-keys config/db-url :users {:slack_id slack-id})))

(defn get-next-user
  "gets next user in chore sequence"
  [chore-order]
  (jdbc/with-db-transaction [t-con config/db-url]
                            (let [max-chore-order (:count (first (jdbc/query t-con (sql/select [`(count :*)] (sql/from :users)))))
                                  next-order (mod (+ 1 (or chore-order 0)) max-chore-order)]
                              (first (jdbc/find-by-keys t-con :users {:chore_order next-order})))))
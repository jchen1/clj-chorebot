(ns clj-chorebot.models.user
  (:require [clj-chorebot.config :as config]
            [clojure.java.jdbc :as jdbc]
            [clj-chorebot.util.sql :as sql]))

(defn create
  "Creates a user"
  [user]
  (jdbc/with-db-transaction
    [t-con config/db-url]
    (let
      [max-order (or (->> (sql/select
                            [:chore-order]
                            (sql/from :users)
                            (sql/where `(= :chore-order ~(sql/inner-select
                                                           [`(max :chore-order)]
                                                           (sql/from :users)))))
                          (sql/query t-con)
                          (first)
                          :chore-order)
                     -1)]
      (sql/insert! t-con :users (assoc user :chore-order (+ 1 max-order))))))

(defn decrement-chore-orders-above-n
  [t-con n]
  (jdbc/execute! t-con ["UPDATE users SET chore_order = chore_order - 1 WHERE chore_order > ?" n]))

(defn remove
  "Removes a user, and decrements chore_order"
  [slack-handle]
  (jdbc/with-db-transaction [t-con config/db-url]
                            (let [[user] (sql/find-by-keys t-con :users {:slack-handle slack-handle})]
                              (when user
                                (jdbc/execute! t-con (sql/delete :users (sql/where `(= :slack-handle ~slack-handle))))
                                (decrement-chore-orders-above-n t-con (:chore_order user))
                                user))))

(defn set-admin
  [slack-handle is-admin]
  (< 0 (first (jdbc/execute! config/db-url
                             (sql/update :users {:is-admin is-admin}
                                         (sql/where `(= :slack-handle ~slack-handle)))))))

(defn get-by-username
  [slack-handle]
  (first (sql/find-by-keys config/db-url :users {:slack-handle slack-handle})))

(defn get-all []
  (sql/query config/db-url (sql/select [:slack-handle :is-admin] (sql/from :users))))

(defn get-by-slack-id
  [slack-id]
  (first (sql/find-by-keys config/db-url :users {:slack-id slack-id})))

(defn get-next-user
  "gets next user in chore sequence"
  [chore-order]
  (jdbc/with-db-transaction [t-con config/db-url]
                            (let [max-chore-order (:count (first (sql/query t-con (sql/select [`(count :*)] (sql/from :users)))))
                                  next-order (mod (+ 1 (or chore-order 0)) max-chore-order)]
                              (first (sql/find-by-keys t-con :users {:chore-order next-order})))))
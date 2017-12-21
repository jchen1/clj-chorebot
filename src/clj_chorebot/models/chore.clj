(ns clj-chorebot.models.chore
  (:require [clj-chorebot.config :as config]
            [clojure.java.jdbc :as jdbc]
            [clj-chorebot.util.sql :as sql]))

(defn get-remove-sql
  [chore-name] (sql/delete :chores (sql/where `(= :name ~chore-name))))

(defn get-by-name
  [name]
  (first (jdbc/find-by-keys config/db-url :chores {:name name})))

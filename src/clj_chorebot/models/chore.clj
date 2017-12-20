(ns clj-chorebot.models.chore
  (:require [clj-chorebot.config :as config]
            [clojure.java.jdbc :as jdbc]
            [java-jdbc.sql :as sql]
            [clj-chorebot.util.sql :as sql-utils]))

(defn create
  "Creates a chore"
  [name description]
  (jdbc/insert! config/db-url :chores {:name name :description description}))

(defn remove
  "Removes a chore"
  [name]
  (jdbc/delete! config/db-url :chores (sql/where {:name name})))

(defn get_by_name
  [name]
  (first (jdbc/find-by-keys config/db-url :chores {:name name})))

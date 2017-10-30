(ns clj-chorebot.migrations
  (:require [clj-chorebot.migrations.init :as init]
            [clj-chorebot.migrations.add_users :as add_users]
            [clj-chorebot.migrations.add_chores :as add_chores]))

(defn migrate []
  (do
    (println "running migrations...")
    (init/migrate)
    (add_users/migrate)
    (add_chores/migrate)
    (println "migrations completed!")))

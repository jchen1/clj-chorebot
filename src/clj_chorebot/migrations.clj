(ns clj-chorebot.migrations
  (:require [clj-chorebot.migrations.init :as init]
            [clj-chorebot.migrations.add-users :as add-users]
            [clj-chorebot.migrations.add-chores :as add-chores]))

(defn migrate []
  (do
    (println "running migrations...")
    (init/migrate)
    (add-users/migrate)
    (add-chores/migrate)
    (println "migrations completed!")))

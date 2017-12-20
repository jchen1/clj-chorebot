(ns clj-chorebot.migrations
  (:require [clj-chorebot.migrations.init :as init]
            [clj-chorebot.migrations.add-users :as add-users]
            [clj-chorebot.migrations.add-chores :as add-chores]
            [clj-chorebot.migrations.add-chorelogs :as add-chorelogs]))

(defn migrate []
  (do
    (println "running migrations...")
    (init/migrate)
    (add-users/migrate)
    (add-chores/migrate)
    (add-chorelogs/migrate)
    (println "migrations completed!")))

(ns clj-chorebot.migrations
  (:require [clj-chorebot.migrations.init :as init]
            [clj-chorebot.migrations.add-users :as add-users]
            [clj-chorebot.migrations.add-chores :as add-chores]
            [clj-chorebot.migrations.add-chorelogs :as add-chorelogs]
            [clj-chorebot.migrations.add-topic :as add-topic]))

(defn migrate []
  (do
    (println "running migrations...")
    (init/migrate)
    (add-users/migrate)
    (add-chores/migrate)
    (add-chorelogs/migrate)
    (add-topic/migrate)
    (println "migrations completed!")))

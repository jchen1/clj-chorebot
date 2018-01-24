(ns clj-chorebot.migrations.add-topic
  (:require [clojure.java.jdbc :as sql]
            [clj-chorebot.config :as config]
            [clojure.string :as str]
            [clj-chorebot.slack :as slack]
            [clj-chorebot.models.chorelog :as chorelog]
            [clj-chorebot.models.user :as user]))

(def migration-name "add-topic")

(defn migrated? []
  (-> (sql/query config/db-url
                 [(str "select count(*) from migrations "
                       "where name='" migration-name "'")])
      first :count pos?))

(defn migrate []
  (when (not (migrated?))
    (print "Setting topic...") (flush)
    (let [all-chores (map #(do {:slack-handle (:slack-handle (user/get-next-user (:chore-order %)))
                                :chore-name (:name %)}) (chorelog/get-last-all))]
      (slack/set-topic config/chores-channel (str/join " | " (map #(format "%s: %s" (:chore-name %) (:slack-handle %)) all-chores))))
    (sql/insert! config/db-url :migrations {:name migration-name})
    (println " done")))

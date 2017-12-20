(ns clj-chorebot.core
  (:require [clj-chorebot.migrations :as migrations]
            [clj-chorebot.slack :as slack]
            [clj-chorebot.handler :as handler])
  (:gen-class))

(def conn (atom {}))

(defn get-conn []
  (when-not (:conn @conn)
    (swap! conn assoc :conn (slack/init-rtm)))
  (:conn @conn))

(defn go []
  (when-not (:sub @conn)
    (swap! conn assoc :sub (slack/subscribe (get-conn) :message handler/handler)))
  (:sub @conn))

(defn -main []
  (do
    (migrations/migrate)
    (let [rtm-conn (slack/init-rtm)]
      (slack/subscribe rtm-conn :message handler/handler))
    (loop [] () (recur))))

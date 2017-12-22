(ns clj-chorebot.core
  (:require [clj-chorebot.migrations :as migrations]
            [clj-chorebot.slack :as slack]
            [clj-chorebot.handler :as handler]
            [clojure.core.async :as async]
            [clj-chorebot.config :as config])
  (:gen-class))

(def conn (atom {}))

(defn get-conn []
  (when-not (:conn @conn)
    (swap! conn assoc :conn (slack/init-rtm)))
  (:conn @conn))

(defn go []
  (async/unsub-all (:events-publication (get-conn)))
  (swap! conn assoc :sub (slack/subscribe (get-conn) :message handler/handler))
  (:sub @conn))

(defn -main []
  (do
    (migrations/migrate)
    (let [rtm-conn (slack/init-rtm)]
      (slack/subscribe rtm-conn :message handler/handler))
    (println (format "initialized: posting to #%s" config/chores-channel))
    (loop [] () (recur))))

(ns clj-chorebot.core
  (:require [clj-chorebot.migrations :as migrations]
            [clj-chorebot.slack :as slack]
            [clj-chorebot.handler :as handler])
  (:gen-class))

(defn -main []
  (do
    (migrations/migrate)
    (let [rtm-conn (slack/init-rtm)]
      (slack/subscribe rtm-conn :message handler/handler))
    (loop [] () (recur))))

(ns clj-chorebot.core
  (:require [clj-chorebot.migrations :as migrations]
            [clj-chorebot.slack :as slack]
            [clj-chorebot.handler :as handler])
  (:gen-class))

(defn -main []
  (do
    (migrations/migrate)
    (let [rtm_conn (slack/init_rtm)]
      (slack/subscribe rtm_conn :message handler/handler))
    (loop [] () (recur))))

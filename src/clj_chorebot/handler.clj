(ns clj-chorebot.handler
  (:require [clj-chorebot.slack :as slack]
            [clj-chorebot.config :as config]
            [clj-chorebot.commands.commands :as commands]
            [clojure.string :as string]
            [clojure.core.match :refer [match]]))

(def ignored_msg_types #{ "bot_message", "channel_topic" })

(defn handler
  [{:keys [channel user subtype text bot_id topic]}]
  "handle slack message"
  (do
    ; (println (str channel " " text " " subtype))
    (if (or (nil? bot_id) (not (nil? topic))) ;ignore if topic is set or bot message
    (let [words (string/split text #" ")
          command (first words)
          args (rest words)]
      (match [command]
        ["info"] (commands/info channel args user)
        ["help"] (commands/help channel args user)
        ["remind"] (commands/remind channel args user)
        ["finished"] (commands/finished channel args user)
        ["add-user"] (commands/add-user channel args user)
        ["remove-user"] (commands/remove-user channel args user)
        ["promote"] (commands/promote channel args user)
        ["demote"] (commands/demote channel args user)
        :else (println (str "bad command: " command " " args))))
    )))

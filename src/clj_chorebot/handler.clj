(ns clj-chorebot.handler
  (:require [clj-chorebot.slack :as slack]
            [clj-chorebot.config :as config]
            [clj-chorebot.commands.commands :as commands]
            [clojure.string :as string]
            [clojure.core.match :refer [match]]))

(defn handler
  [{:keys [channel user subtype text bot_id topic]}]
  "handle slack message"
  (do
    (if (or (nil? bot_id) (not (nil? topic)))               ;ignore if topic is set or bot message
      (let [words (map string/lower-case (string/split text #" "))
            args (rest words)
            command (match [(first words)]
                           ["info"] commands/info
                           ["help"] commands/help
                           ["remind"] commands/remind
                           ["finished"] commands/finished
                           ["add-user"] commands/add-user
                           ["remove-user"] commands/remove-user
                           ["set-turn"] commands/set-turn
                           ["promote"] commands/promote
                           ["demote"] commands/demote
                           ["add-chore"] commands/add-chore
                           ["remove-chore"] commands/remove-chore
                           ["list-users"] commands/list-users
                           :else nil)]
        (when command (command channel args user))))))

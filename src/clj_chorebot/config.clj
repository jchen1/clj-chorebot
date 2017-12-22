(ns clj-chorebot.config
  (:require [environ.core :refer [env]]))

(def db-url (or (env :database-url)
                "postgresql://localhost:5432/chores"))

(def slack-token (or (env :slack-api-token)
                     "xoxb-263654194611-vZj02eznoiSj4TjzEGmmG00g"))

(def chores-channel (if (= (env :is-prod) "true")
                      "chores"
                      "clj-chorebot-test"))

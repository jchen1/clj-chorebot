(ns clj-chorebot.config)

(def db-url (or (System/getenv "DATABASE_URL")
                "postgresql://localhost:5432/chores"))

(def slack_token (or (System/getenv "SLACK_API_TOKEN")
                     "xoxb-263654194611-vZj02eznoiSj4TjzEGmmG00g"))

(def chores_channel (if (= (System/getenv "PROD") "true")
                      "chores"
                      "clj-chorebot-test"))

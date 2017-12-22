(ns clj-chorebot.config
  (:require [environ.core :refer [env]]
            [clojure.java.shell :refer [sh]]))

(def db-url (or (env :database-url)
                "postgresql://localhost:5432/chores"))

(def slack-token (or (env :slack-api-token)
                     "xoxb-263654194611-vZj02eznoiSj4TjzEGmmG00g"))

(def chores-channel (if (= (env :is-prod) "true")
                      "chores"
                      "clj-chorebot-test"))

(def version (env :clj-chorebot-version))

(def git-sha
  (->> ["git" "rev-parse" "HEAD"]
       (apply sh)
       :out
       clojure.string/trim))
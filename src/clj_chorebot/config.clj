(ns clj-chorebot.config
  (:require [environ.core :refer [env]]
            [clojure.java.shell :refer [sh]]))

(def db-url (or (env :database-url)
                "postgresql://localhost:5432/chores"))

(def slack-token (or (env :slack-api-token)
                     "xoxb-263654194611-vZj02eznoiSj4TjzEGmmG00g"))

(def is-prod (= (env :is-prod) "true"))

(def chores-channel (if is-prod
                      "chores"
                      "clj-chorebot-test"))

(def version-file "version.edn")

(defn version [] (env :clj-chorebot-version))

(defn git-sha []
  (->> ["git" "rev-parse" "HEAD"]
       (apply sh)
       :out
       clojure.string/trim))
(ns clj-chorebot.core
  (:require [clj-chorebot.migrations :as migrations]
            [clj-chorebot.slack :as slack]
            [clj-chorebot.handler :as handler]
            [clojure.core.async :as async]
            [clj-chorebot.config :as config]
            [environ.core :refer [env]])
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

(def version
  (->>
    "project.clj"
    slurp
    read-string
    (drop 2)
    (cons :version)
    (apply hash-map)
    :version))

(def git-sha
  (->> ["git" "rev-parse" "HEAD"]
       (apply sh)
       :out
       clojure.string/trim))

(defn -main []
  (do
    (migrations/migrate)
    (let [c (go)]
      (println (format "initialized: posting to #%s" config/chores-channel))
      (slack/post config/chores-channel (format "Deployed version %s (%s) to %s." version git-sha (if (env :is-prod) "prod" "dev")))
      (loop [] (Thread/sleep 1000) (recur)))))

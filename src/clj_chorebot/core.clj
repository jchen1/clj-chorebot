(ns clj-chorebot.core
  (:require [clj-chorebot.migrations :as migrations]
            [clj-chorebot.slack :as slack]
            [clj-chorebot.handler :as handler]
            [clojure.core.async :as async]
            [clj-chorebot.config :as config]
            [environ.core :refer [env]]
            [clj-chorebot.models.user :as user]
            [clj-chorebot.deploy :as deploy]
            [clojure.java.io :as io])
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
  (migrations/migrate)
  (let [{:keys [version git-sha first-deploy?] :as cfg} (read-string (slurp config/version-file))
        admins (map :slack-handle (user/get-admins))
        msg (format "Deployed version %s (%s) to %s." version git-sha (if (env :is-prod) "prod" "dev"))]
    (when first-deploy?
      (run! #(slack/post (:id (slack/get-user-by-handle %)) msg) admins)
      (deploy/write-project-config config/version-file (assoc cfg :first-deploy? false))))
  (go)
  (loop [] (Thread/sleep 1000) (recur)))

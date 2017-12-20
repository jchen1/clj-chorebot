(ns clj-chorebot.commands.commands
  (:require [clj-chorebot.config :as config])
  (:require [clj-chorebot.slack :as slack])
  (:require [clj-chorebot.models.user :as user])
  (:require [clj-chorebot.models.chore :as chore])
  (:require [clj-chorebot.models.chorelog :as chorelog])
  (:require [clojure.core.match :refer [match]])
  (:require [clojure.string :as string]))

(def help-msg (str
                "Available commands:\n"
                "`finished [[chore]|all]`: mark a task as completed\n"
                "`remind [[chore]|all]`: remind the owner of a task\n"
                "`info [chore] (optional)`: list chores and their owners`\n"
                "`help`: print this message"))

(def admin-help-msg (str
                      "`add_user [username]`: add a user to the chore rotation\n"
                      "`remove_user [username]`: remove a user from the chore rotation\n"
                      "`add_chore [chore]`: add a chore to the chore rotation\n"
                      "`remove_chore [chore]`: remove a chore from the chore rotation\n"
                      "`promote [username]`: promote a user to admin\n"
                      "`demote [username]`: demote a user"))

(defn help
  "prints help info"
  [channel args user]
  (slack/post
    channel
    (if (:is_admin (user/get-by-slack-id user))
      (str help-msg "\n\nAdmin commands:\n" admin-help-msg)
      help-msg)))

(defn info
  "prints info about the given chore"
  [channel [chore-name] user]
  (if (= chore-name "all")
    (let [])
    (if-let [{:keys [completed_at chore-order]} (chorelog/get_last chore-name)]
      (slack/post channel (str "@" (:slack_handle (user/get-next-user chore-order)) " is responsible for " chore-name ". (last completed " completed_at ")"))
      (slack/post channel (str "Unrecognized chore: " chore-name ".")))))

; todo
(defn
  finished
  "marks chore as completed"
  [channel [chore-name] slack_id]
  (if (= (:slack_id (chorelog/get_next chore-name)) slack_id)
    (let [next_handle (chorelog/complete_chore chore-name slack_id)]
      (do
        (slack/post config/chores_channel (str "<@" next_handle "> is now responsible for " chore-name "."))))
    (slack/post channel (str chore-name " is not a recognized chore or it's not your turn"))))

(defn remind
  "reminds user about chore"
  [channel [chore-name] user]
  (if (= (or chore-name "all") "all")
    ()                                                      ; todo grab all chores
    (let [{:keys [description completed_at chore-order]} (chorelog/get_last chore-name)]
      (if (nil? description)
        (slack/post channel (str chore-name " is not a recognized chore."))
        (slack/post config/chores_channel (str "<@" (:slack_handle (user/get-next-user chore-order)) ">, please " description))))))

(defn admin-wrapper
  ""
  [f]
  (fn [channel args user]
    (if (:is_admin (user/get-by-slack-id user))
      (f channel args user)
      (slack/post channel "Not authorized."))))

(defn normalize-username
  ""
  [username]
  (if (string/starts-with? username "@")
    (subs username 1)
    username))

(def add-user (admin-wrapper
                (fn [channel [handle] _]
                  (if-let [user_map (slack/get-user-by-handle (normalize-username handle))]
                    (do
                      (user/create {:slack_handle (:name user_map) :slack_id (:id user_map)})
                      (slack/post channel (str "Added user @" handle " to the chore rotation.")))
                    (slack/post channel (str "User @" (normalize-username handle) " is not a recognized user."))))))

(def remove-user (admin-wrapper
                   (fn [channel [handle] _]
                     (do
                       (user/remove handle)
                       (slack/post channel (str "Removed user @" handle " from the chore rotation."))))))

(def promote (admin-wrapper
               (fn [channel [handle] _]
                 (do
                   (user/set-admin handle true)
                   (slack/post channel (str "Set user @" handle " to admin."))))))

(def demote (admin-wrapper
              (fn [channel [handle] _]
                (do
                  (user/set-admin handle false)
                  (slack/post channel (str "Set user @" handle " to normal user."))))))

(def add-chore (admin-wrapper
                 (fn [channel [chore-name] _]
                   ()                                       ; todo
                   )))

(def remove-chore (admin-wrapper
                 (fn [channel [chore-name] _]
                   ()                                       ; todo
                   )))
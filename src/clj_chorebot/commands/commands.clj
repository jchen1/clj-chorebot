(ns clj-chorebot.commands.commands
  (:require [clj-chorebot.config :as config])
  (:require [clj-chorebot.slack :as slack])
  (:require [clj-chorebot.models.user :as user])
  (:require [clj-chorebot.models.chore :as chore])
  (:require [clj-chorebot.models.chorelog :as chorelog])
  (:require [clojure.core.match :refer [match]]))

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
      (str help-msg "\nAdmin commands:\n" admin-help-msg)
      help-msg)))

(defn info
  "prints info about the given chore"
  [channel [chore_name] user]
  (let [{:keys [completed_at chore_order]} (chorelog/get_last chore_name)]
    (slack/post channel (str "@" (:slack_handle (user/get-next-user chore_order)) " is responsible for " chore_name ". (last completed " completed_at ")"))))

; todo
(defn
  finished
  "marks chore as completed"
  [channel [chore_name] slack_id]
  (if (= (:slack_id (chorelog/get_next chore_name)) slack_id)
    (let [next_handle (chorelog/complete_chore chore_name slack_id)]
      (do
        (slack/post config/chores_channel (str "<@" next_handle "> is now responsible for " chore_name "."))
;        (slack/set_topic config/chores_channel) ;TODO
        ))
    (slack/post channel (str chore_name " is not a recognized chore or it's not your turn"))))

(defn remind
  "reminds user about chore"
  [channel [chore_name] user]
  (if (= (or chore_name "all") "all")
    () ; todo grab all chores
    (let [{:keys [description completed_at chore_order]} (chorelog/get_last chore_name)]
      (if (nil? description)
        (slack/post channel (str chore_name " is not a recognized chore."))
        (slack/post config/chores_channel (str "<@" (:slack_handle (user/get-next-user chore_order)) ">, please " description))))))

(defn admin-wrapper
  ""
  [f]
  (fn [channel args user]
    (if (:is_admin (user/get-by-slack-id user))
        (f channel args user)
        (slack/post channel "Not authorized."))))

; todo for user funcs: check handle for existence
; support leading @
(def add-user (admin-wrapper
  (fn [channel [handle] _]
    (let [user_map (slack/get-user-by-handle handle)]
      (do
        (println user_map)
        (user/create {:slack_handle (:name user_map) :slack_id (:id user_map)})
        (slack/post channel (str "Added user @" handle " to the chore rotation.")))))))

(def remove-user (admin-wrapper
  (fn [channel [handle] _]
    (do
      (user/remove handle)
      (slack/post channel (str "Removed user @" handle " from the chore rotation."))))))

(def promote (admin-wrapper
  (fn [channel [handle] _]
    (do
      (user/set-admin handle true)
      (slack/post channel (str "Promoted user @" handle " to admin."))))))

(def demote (admin-wrapper
  (fn [channel [handle] _]
    (do
      (user/set-admin handle false)
      (slack/post channel (str "Demoted user @" handle " to normal user."))))))

(ns clj-chorebot.commands.commands
  (:require [clj-chorebot.slack :as slack])
  (:require [clj-chorebot.models.user :as user])
  (:require [clj-chorebot.models.chore :as chore])
  (:require [clj-chorebot.models.chorelog :as chorelog]))

(def help_msg (str
  "Available commands:\n"
  "`finished [[chore]|all]`: mark a task as completed\n"
  "`remind [[chore]|all]`: remind the owner of a task\n"
  "`info [chore] (optional)`: list chores and their owners`\n"
  "`help`: print this message"))

(def admin_help_msg (str
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
    (if (get (user/get_by_slack_id user) :is_admin)
      (str help_msg "\nAdmin commands:\n" admin_help_msg)
      help_msg)))

(defn info
  "prints info about the given chore"
  [channel [chore_name] user]
  (let [{:keys [completed_at chore_order]} (chorelog/get_last chore_name)]
    (slack/post channel (str "<@" (:slack_handle (user/get_next_user chore_order)) "> is responsible for " chore_name ". (last completed " completed_at ")"))))

; todo
(defn finished "" [] ())
(defn remind "" [] ())


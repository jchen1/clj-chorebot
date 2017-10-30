(ns clj-chorebot.commands.commands
  (:require [clj-chorebot.slack :as slack])
  (:require [clj-chorebot.models.user :as user])
  (:require [clj-chorebot.models.chore :as chore]))

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

; todo (info "${name} is responsible for ${chore}. (last completed ${date})")
(defn info
  "prints info about the given chore"
  [channel [chore] user]
  (let [description (get (chore/get_by_name chore) :description)]
    (slack/post channel description)))

; todo
(defn finished "" [] ())
(defn remind "" [] ())


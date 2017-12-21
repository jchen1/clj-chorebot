(ns clj-chorebot.commands.commands
  (:require [clj-chorebot.config :as config])
  (:require [clj-chorebot.slack :as slack])
  (:require [clj-chorebot.models.user :as user])
  (:require [clj-chorebot.models.chore :as chore])
  (:require [clj-chorebot.models.chorelog :as chorelog])
  (:require [clj-chorebot.services.chore-service :as chore-service])
  (:require [clojure.core.match :refer [match]])
  (:require [clojure.string :as string]
            [clojure.string :as str]))

(defn format-error [format-str & params]
  (apply format (str "Error: " format-str) params))

(def help-msg (str
                "Available commands:\n"
                "`finished [[chore]|all]`: mark a task as completed\n"
                "`remind [[chore]|all]`: remind the owner of a task\n"
                "`info [chore] (optional)`: list chores and their owners`\n"
                "`help`: print this message"))

(def admin-help-msg (str
                      "`add-user [username]`: add a user to the chore rotation\n"
                      "`remove-user [username]`: remove a user from the chore rotation\n"
                      "`add-chore [chore]`: add a chore to the chore rotation\n"
                      "`remove-chore [chore]`: remove a chore from the chore rotation\n"
                      "`promote [username]`: promote a user to admin\n"
                      "`demote [username]`: demote a user"))

(defn help
  "prints help info"
  [channel args user]
  (slack/post
    channel
    (if (:is_admin (user/get-by-slack-id user))
      (format "%s\n\nAdmin commands:\n%s" help-msg admin-help-msg)
      help-msg)))

(defn info
  "prints info about the given chore"
  [channel [chore-name] user]
  (let [chore-name (or chore-name "all")
        chore-info (if (= chore-name "all") (chorelog/get-last-all) (chorelog/get-last chore-name))]
    (if (not-empty chore-info)
      (slack/post channel (str/join "\n" (map (fn [{:keys [completed_at chore_order name]}]
                                                (format "@%s is reponsible for %s. (last completed %s)"
                                                        (:slack_handle (user/get-next-user chore_order))
                                                        name
                                                        completed_at)) chore-info)))
      (slack/post channel (format-error "%s isn't a chore" chore-name)))))

(defn
  finished
  "marks chore as completed"
  [channel [chore-name] slack_id]
  (if (= (:slack_id (chorelog/get-next chore-name)) slack_id)
    (let [next_handle (chorelog/complete_chore chore-name slack_id)]
      (do
        (slack/post config/chores-channel (str "<@" next_handle "> is now responsible for " chore-name "."))))
    (slack/post channel (str chore-name " is not a recognized chore or it's not your turn"))))

(defn remind
  "reminds user about chore"
  [channel [chore-name] user]
  (if chore-name
    (let [chore-info (if (= chore-name "all") (chorelog/get-last-all) (chorelog/get-last chore-name))]
      (if (not-empty chore-info)
        (slack/post channel (str/join "\n" (map (fn [{:keys [description completed_at chore_order]}]
                                                  (format "<@%s>, please %s" (:slack_handle (user/get-next-user chore_order)) description)) chore-info)))
        ((slack/post channel (format-error "%s is not a recognized chore." chore-name)))))
    (slack/post channel (format-error "`remind` requires a chore name or `all`"))))

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
                  (if-let [handle (normalize-username handle)]
                    (if-let [{:keys [name id]} (slack/get-user-by-handle handle)]
                      (do
                        (user/create {:slack_handle name :slack_id id})
                        (slack/post channel (format "Added @%s to the chore rotation" handle)))
                      (slack/post channel (format-error "@%s is not a valid user" handle)))
                    (slack/post channel (format-error "`add-user` requires a username"))))))

(def remove-user (admin-wrapper
                   (fn [channel [handle] _]
                     (if-let [handle (normalize-username handle)]
                       (if (user/remove handle)
                         (slack/post channel (format "Removed @%s from the chore rotation" handle))
                         (slack/post channel (format-error "@%s is not a valid user" handle)))
                       (slack/post channel (format-error "`add-user` requires a username"))))))

(def promote (admin-wrapper
               (fn [channel [handle] _]
                 (if-let [handle (normalize-username handle)]
                   (if (user/set-admin handle true)
                     (slack/post channel (format "Added admin to @%s" handle))
                     (slack/post channel (format-error "@%s is not a valid user" handle)))
                   (slack/post channel (format-error "`promote` requires a username"))))))

(def demote (admin-wrapper
              (fn [channel [handle] _]
                (if-let [handle (normalize-username handle)]
                  (if (user/set-admin handle false)
                    (slack/post channel (format "Removed admin from @%s" handle))
                    (slack/post channel (format-error "@%s is not a valid user" handle)))
                  (slack/post channel (format-error "`demote` requires a username"))))))

(def add-chore (admin-wrapper
                 (fn [channel [chore-name & description] _]
                   (let [description (str/join " " description)
                         _ (println chore-name description)]
                     (if (and chore-name description)
                       (if (chore-service/add chore-name description)
                         (slack/post channel (format "Added chore %s" chore-name))
                         (slack/post channel (format-error "couldn't add %s: probably a duplicate" chore-name))) ;todo
                       (slack/post channel (format-error "`add-chore` requires a name and description")))))))

(def remove-chore (admin-wrapper
                 (fn [channel [chore-name] _]
                   (if (chore-service/remove chore-name)
                     (slack/post channel (format "Removed chore %s" chore-name))
                     (slack/post channel (format-error "%s is not a chore" chore-name))))))
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

(def help-msg
  (str/join "\n"
            ["Available commands:\n"
             "`finished [[chore]|all]`: mark a task as completed\n"
             "`remind [[chore]|all]`: remind the owner of a task\n"
             "`info [chore] (optional)`: list chores and their owners\n"
             "`help`: print this message"]))

(def admin-help-msg
  (str/join "\n"
            ["`add-user [username]`: add a user to the chore rotation"
             "`remove-user [username]`: remove a user from the chore rotation"
             "`add-chore [chore]`: add a chore to the chore rotation"
             "`remove-chore [chore]`: remove a chore from the chore rotation"
             "`promote [username]`: promote a user to admin"
             "`demote [username]`: demote a user"
             "`list-users`: list all users and their type"]))

(defn help
  "prints help info"
  [channel args user]
  (slack/post
    channel
    (if (:is-admin (user/get-by-slack-id user))
      (format "%s\n\nAdmin commands:\n%s" help-msg admin-help-msg)
      help-msg)))

(defn info
  "prints info about the given chore"
  [channel [chore-name] user]
  (let [chore-name (or chore-name "all")
        chore-info (if (= chore-name "all") (chorelog/get-last-all) (chorelog/get-last chore-name))]
    (if (not-empty chore-info)
      (slack/post channel (str/join "\n" (map (fn [{:keys [completed-at chore-order name]}]
                                                (format "@%s is reponsible for %s. (last completed %s)"
                                                        (:slack-handle (user/get-next-user chore-order))
                                                        name
                                                        completed-at)) chore-info)))
      (slack/post channel (format-error "%s isn't a chore" chore-name)))))

(defn
  finished
  "marks chore as completed"
  [channel [chore-name] slack-id]
  (if (= (:slack-id (chorelog/get-next chore-name)) slack-id)
    (let [next-handle (chorelog/complete-chore chore-name slack-id)]
      (do
        (slack/post config/chores-channel (str "<@" next-handle "> is now responsible for " chore-name "."))))
    (slack/post channel (str chore-name " is not a recognized chore or it's not your turn"))))

(defn remind
  "reminds user about chore"
  [channel [chore-name] user]
  (if chore-name
    (let [chore-info (if (= chore-name "all") (chorelog/get-last-all) (chorelog/get-last chore-name))]
      (if (not-empty chore-info)
        (slack/post channel (str/join "\n" (map (fn [{:keys [description completed-at chore-order]}]
                                                  (format "<@%s>, please %s" (:slack-handle (user/get-next-user chore-order)) description)) chore-info)))
        ((slack/post channel (format-error "%s is not a recognized chore." chore-name)))))
    (slack/post channel (format-error "`remind` requires a chore name or `all`"))))

(defn admin-wrapper
  ""
  [f]
  (fn [channel args user]
    (if (:is-admin (user/get-by-slack-id user))
      (f channel args user)
      (slack/post channel "Not authorized."))))

(defn normalize-username
  ""
  [username]
  (if (string/starts-with? username "<")                    ;@name in slack goes to <@id>
    (:name (slack/find-first
             slack/user-cache
             #(= (:id %) (string/replace username #"[@<>]" ""))))
    username))

(def add-user (admin-wrapper
                (fn [channel [handle] _]
                  (if-let [handle (normalize-username handle)]
                    (if-let [{:keys [name id]} (slack/get-user-by-handle handle)]
                      (do
                        (user/create {:slack-handle name :slack-id id})
                        (slack/post channel (format "Added @%s to the chore rotation" handle)))
                      (slack/post channel (format-error "@%s is not a valid slack user" handle)))
                    (slack/post channel (format-error "`add-user` requires a username"))))))

(def remove-user (admin-wrapper
                   (fn [channel [handle] _]
                     (if-let [handle (normalize-username handle)]
                       (if (user/remove handle)
                         (slack/post channel (format "Removed @%s from the chore rotation" handle))
                         (slack/post channel (format-error "@%s is not a chorebot user" handle)))
                       (slack/post channel (format-error "`add-user` requires a username"))))))

(def promote (admin-wrapper
               (fn [channel [handle] _]
                 (if-let [handle (normalize-username handle)]
                   (if (user/set-admin handle true)
                     (slack/post channel (format "Added admin to @%s" handle))
                     (slack/post channel (format-error "@%s is not a chorebot user" handle)))
                   (slack/post channel (format-error "`promote` requires a username"))))))

(def demote (admin-wrapper
              (fn [channel [handle] _]
                (if-let [handle (normalize-username handle)]
                  (if (user/set-admin handle false)
                    (slack/post channel (format "Removed admin from @%s" handle))
                    (slack/post channel (format-error "@%s is not a chorebot user" handle)))
                  (slack/post channel (format-error "`demote` requires a username"))))))

(def add-chore (admin-wrapper
                 (fn [channel [chore-name & description] _]
                   (let [description (str/join " " description)]
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

(def list-users (admin-wrapper
                  (fn [channel _ _]
                    (->>
                      (user/get-all)
                      (map #(format "@%s: %s" (:slack-handle %) (if (:is-admin %) "admin" "user")))
                      (str/join "\n")
                      (slack/post channel)))))
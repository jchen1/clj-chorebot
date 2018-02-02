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
             #(= (:id %) (string/upper-case (string/replace username #"[@<>]" "")))))
    username))

(def help-msg
  (str/join "\n"
            ["Available commands:"
             "`finished [[chore]|all]`: mark a task as completed"
             "`remind [[chore]|all]`: remind the owner of a task"
             "`info [chore (optional)]`: list chores and their owners"
             "`help`: print this message"]))

(def admin-help-msg
  (str/join "\n"
            ["`set-turn [chore] [username]`: set chore to be specified user's responsibility"
             "`add-user [username]`: add a user to the chore rotation"
             "`remove-user [username]`: remove a user from the chore rotation"
             "`add-chore [chore] [description]`: add a chore to the chore rotation"
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
        chore-info (if (= chore-name "all") (chorelog/get-last-all) [(chorelog/get-last chore-name)])]
    (if (not-empty chore-info)
      (slack/post channel (str/join "\n" (map (fn [{:keys [completed-at chore-order name]}]
                                                (format "@%s is responsible for %s. (last completed %s)"
                                                        (:slack-handle (user/get-next-user chore-order))
                                                        name
                                                        completed-at)) chore-info)))
      (slack/post channel (format-error "%s isn't a chore" chore-name)))))

(defn set-topic
  []
  (let [all-chores (sort-by :chore-name (map #(do {:slack-handle (:slack-handle (user/get-next-user (:chore-order %)))
                                                   :chore-name (:name %)}) (chorelog/get-last-all)))]
    (slack/set-topic config/chores-channel (str/join " | " (map #(format "%s: %s" (:chore-name %) (:slack-handle %)) all-chores)))))

(defn finished
  "marks chore as completed"
  [channel [chore-name] slack-id]
  (if-let [next-user (chorelog/get-next chore-name)]
    (if (= (:slack-id next-user) slack-id)
      (let [next-handle (chorelog/complete-chore chore-name slack-id)]
        (slack/post config/chores-channel (format "Thanks! <@%s> is now responsible for %s." next-handle chore-name))
        (set-topic))
      (slack/post channel (format-error "It's not your turn to do %s." chore-name)))
    (slack/post channel (format-error "`%s` is not a valid chore name." chore-name))))

(def set-turn
  (admin-wrapper
    (fn [channel [chore-name username] _]
      (if (and chore-name username)
        (if-let [user (user/get-by-username (normalize-username username))]
          (if-let [chore (chore/get-by-name chore-name)]
            (let [slack-handle (chorelog/set-turn chore user)]
              (slack/post config/chores-channel (format "<@%s> is now responsible for %s." slack-handle chore-name))
              (set-topic))
            (slack/post channel (format-error "`%s` is not a valid chore name." chore-name)))
          (slack/post channel (format-error "@%s is not a chorebot user." username)))
        (slack/post channel (format-error "`chore` and `username` are required."))))))

(defn remind
  "reminds user about chore"
  [channel [chore-name] user]
  (if chore-name
    (let [chore-info (if (= chore-name "all") (chorelog/get-last-all) [(chorelog/get-last chore-name)])]
      (if (not-empty (filter some? chore-info))
        (slack/post config/chores-channel (str/join "\n" (map (fn [{:keys [description completed-at chore-order]}]
                                                                (format "<@%s>, please %s" (:slack-handle (user/get-next-user chore-order)) description)) chore-info)))
        ((slack/post channel (format-error "%s is not a recognized chore." chore-name)))))
    (slack/post channel (format-error "`remind` requires a chore name or `all`"))))

(def add-user (admin-wrapper
                (fn [channel [handle] _]
                  (if-let [handle (normalize-username handle)]
                    (if-let [{:keys [name id]} (slack/get-user-by-handle handle)]
                      (do
                        (user/create {:slack-handle name :slack-id id})
                        (slack/post channel (format "Added @%s to the chore rotation" handle))
                        (set-topic))
                      (slack/post channel (format-error "@%s is not a valid slack user" handle)))
                    (slack/post channel (format-error "`add-user` requires a username"))))))

(def remove-user (admin-wrapper
                   (fn [channel [handle] _]
                     (if-let [handle (normalize-username handle)]
                       (if (user/remove handle)
                         (do
                           (slack/post channel (format "Removed @%s from the chore rotation" handle))
                           (set-topic))
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
                   (if chore-name
                     (if-not (empty? (keep identity description))
                       (let [description (str/join " " description)]
                         (if (chore-service/add chore-name description)
                           (do
                             (slack/post channel (format "Added chore %s" chore-name))
                             (set-topic))
                           (slack/post channel (format-error "couldn't add %s: probably a duplicate" chore-name))))
                       (slack/post channel (format-error "`description` is required")))
                     (slack/post channel (format-error "`name` is required"))))))

(def remove-chore (admin-wrapper
                 (fn [channel [chore-name] _]
                   (if (chore-service/remove chore-name)
                     (do
                       (slack/post channel (format "Removed chore %s" chore-name))
                       (set-topic))
                     (slack/post channel (format-error "%s is not a chore" chore-name))))))

(def list-users (admin-wrapper
                  (fn [channel _ _]
                    (->>
                      (user/get-all)
                      (map #(format "@%s: %s" (:slack-handle %) (if (:is-admin %) "admin" "user")))
                      (str/join "\n")
                      (slack/post channel)))))
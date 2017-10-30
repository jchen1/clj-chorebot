(ns clj-chorebot.slack
  (:require [clj-chorebot.config :as config]
            [clj-slack.users :as slack.users]
            [clj-slack.channels :as slack.channels]
            [clj-slack.chat :as slack.chat]
            [clj-slack.im :as slack.im]
            [slack-rtm.core :as slack-rtm]))


(def api-conn {:api-url "https://slack.com/api" :token config/slack_token})
(def channel-cache (:channels (slack.channels/list api-conn)))
(def dm-cache (:ims (slack.im/list api-conn)))
(def user-cache (:members (slack.users/list api-conn)))

(defn find-first
  [coll f]
  (first (filter f coll)))

(defn init-rtm
  "init slack rtm"
  []
  (slack-rtm/connect config/slack_token))

(defn get-channel-id
  "takes channel name or id and returns channel id"
  [channel_name]
  (or (:id (find-first channel-cache (fn [c] (= (:name c) channel_name)))) channel_name))

(defn get-dm-for-user
  [user_id]
  (:id (find-first dm-cache (fn [i] (= (:user i) user_id)))))

(defn subscribe
  ""
  [rtm_conn type handler]
  (let [events_publication (:events-publication rtm_conn)]
    (slack-rtm/sub-to-event events_publication type handler)))

(defn post
  ""
  [channel_name msg]
  (slack.chat/post-message api-conn (get-channel-id channel_name) msg {:as_user "true"}))

(defn set_topic
  ""
  [channel_name topic]
  (slack.channels/set-topic api-conn (get-channel-id channel_name) topic))

(defn get-user-by-handle
  ""
  [handle]
  (find-first user-cache (fn [u] (= (:name u) handle))))

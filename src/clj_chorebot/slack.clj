(ns clj-chorebot.slack
  (:require [clj-chorebot.config :as config]
            [clj-slack.users :as slack.users]
            [clj-slack.channels :as slack.channels]
            [clj-slack.chat :as slack.chat]
            [clj-slack.im :as slack.im]
            [slack-rtm.core :as slack-rtm]))


(def api-conn {:api-url "https://slack.com/api" :token config/slack-token})
(def channel-cache (:channels (slack.channels/list api-conn)))
(def dm-cache (:ims (slack.im/list api-conn)))
(def user-cache (:members (slack.users/list api-conn)))

(defn find-first
  [coll f]
  (first (filter f coll)))

(defn init-rtm
  "init slack rtm"
  []
  (slack-rtm/connect config/slack-token))

(defn get-channel-id
  "takes channel name or id and returns channel id"
  [channel-name]
  (or (:id (find-first channel-cache (fn [c] (= (:name c) channel-name)))) channel-name))

(defn get-dm-for-user
  [user-id]
  (:id (find-first dm-cache (fn [i] (= (:user i) user-id)))))

(defn subscribe
  ""
  [rtm-conn type handler]
  (let [events-publication (:events-publication rtm-conn)]
    (slack-rtm/sub-to-event events-publication type handler)))

(defn post
  ""
  [channel-name msg]
  (slack.chat/post-message api-conn (get-channel-id channel-name) msg {:as_user "true"}))

(defn set-topic
  ""
  [channel-name topic]
  (slack.channels/set-topic api-conn (get-channel-id channel-name) topic))

(defn get-user-by-handle
  ""
  [handle]
  (find-first user-cache (fn [u] (= (:name u) handle))))

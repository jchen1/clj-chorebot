(ns clj-chorebot.slack
  (:require [clj-chorebot.config :as config]
            [clj-slack.users :as slack.users]
            [clj-slack.channels :as slack.channels]
            [clj-slack.chat :as slack.chat]
            [slack-rtm.core :as slack-rtm]))


(def api_conn {:api-url "https://slack.com/api" :token config/slack_token})
(def channel_cache (get (slack.channels/list api_conn) :channels))

(defn find_first
  [coll f]
  (first (filter f coll)))

(defn init_rtm
  "init slack rtm"
  []
  (slack-rtm/connect config/slack_token))

(defn get_channel_id
  "takes channel name or id and returns channel id"
  [channel_name]
  (or (get (find_first channel_cache (fn [c] (= (get c :name) channel_name))) :id) channel_name))

(defn subscribe
  ""
  [rtm_conn type handler]
  (let [events_publication (:events-publication rtm_conn)]
    (slack-rtm/sub-to-event events_publication type handler)))

(defn post
  ""
  [channel_name msg]
  (slack.chat/post-message api_conn (get_channel_id channel_name) msg {:as_user "true"}))

(defn set_topic
  ""
  [channel_name topic]
  (slack.channels/set-topic api_conn (get_channel_id channel_name) topic))

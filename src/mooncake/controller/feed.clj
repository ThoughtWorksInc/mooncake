(ns mooncake.controller.feed
  (:require [mooncake.activity :as a]
            [mooncake.db.user :as user]
            [mooncake.helper :as mh]
            [mooncake.controller.customise-feed :as cfc]
            [mooncake.view.feed :as f]))

(defn activity-src-preferences->feed-query [preferences-for-an-activity-src]
  (let [selected-types (map :id (filter :selected (:activity-types preferences-for-an-activity-src)))]
    (when-not (empty? selected-types)
      {:activity-src     (name (:id preferences-for-an-activity-src))
       (keyword "@type") selected-types})))

(defn generate-feed-query [feed-settings activity-sources]
  (remove nil? (map activity-src-preferences->feed-query (cfc/generate-activity-source-preferences activity-sources feed-settings))))

(defn feed [store request]
  (let [context (:context request)
        username (get-in request [:session :username])
        user (user/find-user store username)
        user-feed-settings (:feed-settings user)
        activity-sources (:activity-sources context)
        feed-query (generate-feed-query user-feed-settings activity-sources)
        activities (a/retrieve-activities store feed-query)
        updated-context (assoc context :activities activities)]
    (mh/enlive-response (f/feed (assoc request :context updated-context)) (:context request))))
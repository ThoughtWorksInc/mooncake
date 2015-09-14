(ns mooncake.controller.feed
  (:require [mooncake.activity :as a]
            [mooncake.db.user :as user]
            [mooncake.helper :as mh]
            [mooncake.view.feed :as f]
            [mooncake.controller.customise-feed :as cfc]))

(defn activity-source-preferences->active-activity-source-keys [activity-source-preferences]
  (->> (map (fn [a] (when (:selected a) (:id a))) activity-source-preferences)
       (remove nil?)))

(defn get-active-activity-source-keys [user-feed-settings activity-sources]
  (let [activity-source-preferences (cfc/generate-activity-source-preferences activity-sources user-feed-settings)]
    (activity-source-preferences->active-activity-source-keys activity-source-preferences)))

(defn retrieve-activities-from-user-sources [db active-activity-source-keys]
  (a/retrieve-activities-from-database-by-activity-source db active-activity-source-keys))

(defn feed [db request]
  (let [context (:context request)
        username (get-in request [:session :username])
        user (user/find-user db username)
        user-feed-settings (:feed-settings user)
        activity-sources (:activity-sources context)
        active-activity-source-keys (get-active-activity-source-keys user-feed-settings activity-sources)
        activities (retrieve-activities-from-user-sources db active-activity-source-keys)
        updated-context (assoc context :activities activities
                                       :active-activity-source-keys active-activity-source-keys)]
    (mh/enlive-response (f/feed (assoc request :context updated-context)) (:context request))))

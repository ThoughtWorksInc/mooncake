(ns mooncake.controller.feed
  (:require [mooncake.activity :as a]
            [mooncake.db.user :as user]
            [mooncake.helper :as mh]
            [mooncake.view.feed :as f]
            [mooncake.controller.customise-feed :as cfc]))

(defn activity-source-preferences->active-activity-source-keys [activity-source-preferences]
  (->> (map (fn [a] (when (:selected a) (:id a))) activity-source-preferences)
       (remove nil?)))

(defn retrieve-activities-from-user-sources [db user-feed-settings activity-sources]
  (let [activity-source-preferences (cfc/generate-activity-source-preferences activity-sources user-feed-settings)
        active-activity-source-keys (activity-source-preferences->active-activity-source-keys activity-source-preferences)]
    (a/retrieve-activities-from-database-by-activity-source db active-activity-source-keys)))

(defn feed [db request]
  (let [username (get-in request [:session :username])
        user (user/find-user db username)
        user-feed-settings (:feed-settings user)
        activity-sources (get-in request [:context :activity-sources])
        activities (retrieve-activities-from-user-sources db user-feed-settings activity-sources)]
    (mh/enlive-response (f/feed (assoc-in request [:context :activities] activities)) (:context request))))

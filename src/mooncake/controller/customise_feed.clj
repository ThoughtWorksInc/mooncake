(ns mooncake.controller.customise-feed
  (:require [mooncake.db.user :as user]
            [mooncake.helper :as mh]
            [mooncake.view.customise-feed :as cfv]))

(def default-feed-selected-value true)

(defn customise-feed [db request]
  (let [username (get-in request [:session :username])
        activity-source-keys (keys (get-in request [:context :activity-sources]))
        posted-activity-source-keys (keys (select-keys (:params request) activity-source-keys))
        feed-settings-false (zipmap activity-source-keys (repeat false))
        feed-settings-submitted (zipmap posted-activity-source-keys (repeat true))
        feed-settings-combined (merge feed-settings-false feed-settings-submitted)]
    (user/update-feed-settings! db username feed-settings-combined)
    (mh/redirect-to request :feed)))

(defn selected-feed? [a-feed-setting]
  (case a-feed-setting
    true true
    false false
    default-feed-selected-value))

(defn generate-activity-source-preferences [activity-sources user-feed-settings]
  (->> (map (fn [[k v]] (assoc v :id (name k) :selected (selected-feed? (k user-feed-settings)))) activity-sources)
       (sort-by :name)))

(defn show-customise-feed [db request]
  (let [context (:context request)
        username (get-in request [:session :username])
        user (user/find-user db username)
        user-feed-settings (:feed-settings user)
        activity-source-preferences (generate-activity-source-preferences (:activity-sources context) user-feed-settings)
        updated-request (assoc-in request [:context :activity-source-preferences] activity-source-preferences)]
    (mh/enlive-response (cfv/customise-feed updated-request) context)))
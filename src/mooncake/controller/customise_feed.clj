(ns mooncake.controller.customise-feed
  (:require [mooncake.db.user :as user]
            [mooncake.helper :as mh]
            [mooncake.view.customise-feed :as cfv]))

(def default-feed-selected-value true)

(defn create-user-feed-settings [activity-sources posted-activity-source-keys]
  (reduce (fn [user-feed-settings activity-source-key]
            (assoc user-feed-settings activity-source-key {:selected (some? (some #{activity-source-key} posted-activity-source-keys))}))
          {} (keys activity-sources)))

(defn customise-feed [db request]
  (let [username (get-in request [:session :username])
        activity-sources (get-in request [:context :activity-sources])
        posted-activity-source-keys (keys (select-keys (:params request) (keys activity-sources)))
        feed-settings-combined (create-user-feed-settings activity-sources posted-activity-source-keys)]
    (user/update-feed-settings! db username feed-settings-combined)
    (mh/redirect-to request :feed)))

(defn selected-feed? [a-feed-setting]
  (case a-feed-setting
    true true
    false false
    default-feed-selected-value))

(defn get-feed-preferences-for-activity-type [user-feed-activity-type-settings activity-type-id]
  (if-let [feed-preferences-for-activity-type (first (filter (fn[v] (= (:id v) activity-type-id)) user-feed-activity-type-settings))]
    feed-preferences-for-activity-type
    { :id activity-type-id :selected true}))

(defn generate-activity-type-preferences [avaiable-acitivty-type-from-source user-feed-activity-type-settings is-feed-selected?]
  (map (fn [v] {:name v
                :selected (and is-feed-selected? (:selected (get-feed-preferences-for-activity-type user-feed-activity-type-settings v)))})
       avaiable-acitivty-type-from-source))

(defn get-feed-preferences-for-activity-source [user-feed-settings activity-source-id]
  (if-let [feed-preferences-for-activity-source (activity-source-id user-feed-settings)]
    feed-preferences-for-activity-source
    {}))

(defn generate-activity-source-preferences [activity-sources user-feed-settings]
  (->> (map (fn [[k v]] (let [preferences-for-activity-source (get-feed-preferences-for-activity-source user-feed-settings k)
                              is-feed-selected? (selected-feed? (:selected preferences-for-activity-source))]
                          (assoc v
                            :id (name k)
                            :selected is-feed-selected?
                            :activity-types (generate-activity-type-preferences
                                              (:activity-types v)
                                              (:types preferences-for-activity-source)
                                              is-feed-selected?))))
            activity-sources)
       (sort-by :name)))

(defn show-customise-feed [db request]
  (let [context (:context request)
        username (get-in request [:session :username])
        user (user/find-user db username)
        user-feed-settings (:feed-settings user)
        activity-source-preferences (generate-activity-source-preferences (:activity-sources context) user-feed-settings)
        updated-request (assoc-in request [:context :activity-source-preferences] activity-source-preferences)]
    (mh/enlive-response (cfv/customise-feed updated-request) context)))
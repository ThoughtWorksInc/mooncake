(ns mooncake.controller.customise-feed
  (:require [mooncake.db.user :as user]
            [mooncake.helper :as mh]
            [mooncake.view.customise-feed :as cfv]
            [mooncake.db.activity :as a]))

(def default-feed-type-selected-value true)

(defn- construct-activity-type-form-parameter [activity-source-id activity-type]
  (keyword (str (name activity-source-id) "_-_" activity-type)))

(defn- activity-type-submitted? [activity-source-id activity-type posted-parameters]
  (let [activity-type-form-id (construct-activity-type-form-parameter activity-source-id activity-type)]
    (contains? posted-parameters activity-type-form-id)))

(defn create-user-feed-settings-for-source [activity-source-id single-activity-source-configuration posted-parameters]
  (let [activity-types (:activity-types single-activity-source-configuration)
        activity-types->feed-settings-fn (fn [activity-type] {:id       activity-type
                                                              :selected (activity-type-submitted? activity-source-id activity-type posted-parameters)})
        feed-settings-for-types (map activity-types->feed-settings-fn activity-types)]
    {:types feed-settings-for-types}))

(defn add-feed-settings-for-single-activity-source [activity-sources posted-parameters user-feed-settings activity-source-key]
  (let [single-activity-source-configuration (activity-source-key activity-sources)
        feed-settings-for-source (create-user-feed-settings-for-source activity-source-key single-activity-source-configuration posted-parameters)]
    (assoc user-feed-settings activity-source-key feed-settings-for-source)))

(defn create-user-feed-settings [activity-sources posted-parameters]
  (reduce (partial add-feed-settings-for-single-activity-source activity-sources posted-parameters)
          {}
          (keys activity-sources)))

(defn customise-feed [store request]
  (let [username (get-in request [:session :username])
        activity-sources (get-in request [:context :activity-sources])
        feed-settings-combined (create-user-feed-settings activity-sources (:params request))]
    (user/update-feed-settings! store username feed-settings-combined)
    (mh/redirect-to request :feed)))

(defn selected-feed-type? [a-feed-setting]
  (case a-feed-setting
    true true
    false false
    default-feed-type-selected-value))

(defn user-preference-for-activity-type [user-feed-activity-type-settings activity-type]
  (let [current-user-preference (-> (some #(when (= activity-type (:id %)) %) user-feed-activity-type-settings)
                                    :selected) ]
    (selected-feed-type? current-user-preference)))

(defn generate-activity-type-preferences [available-activity-types-from-source user-feed-activity-type-settings]
  (->> (map (fn [activity-type] {:id       activity-type
                                 :selected (user-preference-for-activity-type user-feed-activity-type-settings activity-type)})
            available-activity-types-from-source)
       (sort-by :id)))

(defn get-feed-preferences-for-activity-source [user-feed-settings activity-source-id]
  (if-let [feed-preferences-for-activity-source (activity-source-id user-feed-settings)]
    feed-preferences-for-activity-source
    {}))

(defn generate-activity-source-preferences [activity-sources user-feed-settings]
  (->> (map (fn [[k v]] (let [preferences-for-activity-source (get-feed-preferences-for-activity-source user-feed-settings k)]
                          (assoc v
                            :id (name k)
                            :activity-types (generate-activity-type-preferences
                                              (:activity-types v)
                                              (:types preferences-for-activity-source)))))
            activity-sources)
       (sort-by :name)))

(defn show-customise-feed [store request]
  (let [context (:context request)
        username (get-in request [:session :username])
        user (user/find-user store username)
        user-feed-settings (:feed-settings user)
        activity-source-preferences (generate-activity-source-preferences (:activity-sources context) user-feed-settings)
        updated-request (assoc-in request [:context :activity-source-preferences] activity-source-preferences)]
    (mh/enlive-response (cfv/customise-feed updated-request) context)))
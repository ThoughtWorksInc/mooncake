(ns mooncake.controller.customise-feed
  (:require [mooncake.db.user :as user]
            [mooncake.helper :as mh]
            [mooncake.view.customise-feed :as cfv]))

(def default-feed-selected-value true)

(defn any-true? [collection]
  (true? (some true? collection)))

(defn- get-activity-type-id [activity-source-id activity-type]
  (str (name activity-source-id) "::" activity-type))

(defn- element-id->element-before-id [activity-element-id]
  (str activity-element-id "::before"))

(defn- was-selected-before? [posted-parameters element-before-id]
  (= (get posted-parameters element-before-id) "true"))

(defn- is-checkbox-selected? [posted-parameters checkbox-name]
  (or (contains? posted-parameters checkbox-name)
      (contains? posted-parameters (name checkbox-name))))

(defn- is-type-id-submitted? [activity-source-id activity-type posted-parameters]
  (let [activity-type-form-id (get-activity-type-id activity-source-id activity-type)]
    (is-checkbox-selected? posted-parameters activity-type-form-id)))

(defn is-activity-type-switched-on? [activity-source-id posted-parameters activity-type]
  (let [activity-type-id (get-activity-type-id activity-source-id activity-type)
        activity-type-before-id (element-id->element-before-id activity-type-id)
        posted-activity-type-state (is-checkbox-selected? posted-parameters activity-type-id)
        previous-activity-type-state (was-selected-before? posted-parameters activity-type-before-id)]
    (and posted-activity-type-state (not previous-activity-type-state))))

(defn activity-types-swtiched-on [activity-source-id activity-source-types posted-parameters]
  (map (partial is-activity-type-switched-on? activity-source-id posted-parameters) activity-source-types))

(defn has-any-activity-type-been-switched-on [activity-source-id activity-source-types posted-parameters]
  (any-true? (activity-types-swtiched-on activity-source-id activity-source-types posted-parameters)))

(defn has-activity-source-been-switched-off [activity-source-id posted-parameters]
  (let [activity-source-before-id (element-id->element-before-id (name activity-source-id))
        posted-activity-source-state (is-checkbox-selected? posted-parameters activity-source-id)
        previous-activity-source-state (was-selected-before? posted-parameters activity-source-before-id)]
    (and previous-activity-source-state (not posted-activity-source-state))))

(defn is-activity-source-selected? [activity-source-id activity-source-types posted-parameters]
  (or (is-checkbox-selected? posted-parameters activity-source-id)
      (and (has-any-activity-type-been-switched-on activity-source-id activity-source-types posted-parameters)
           (not (has-activity-source-been-switched-off activity-source-id posted-parameters)))))

(defn create-user-feed-settings-for-source [activity-source-id single-activity-source-configuration posted-parameters]
  (let [activity-source-types (get-in single-activity-source-configuration [:activity-types])
        is-activity-source-selected? (is-activity-source-selected? activity-source-id activity-source-types posted-parameters)
        activity-source-types->feed-settings-fn (fn [v] {:id       v
                                                         :selected (and is-activity-source-selected?
                                                                        (is-type-id-submitted? activity-source-id v posted-parameters))})
        feed-settings-for-types (map activity-source-types->feed-settings-fn activity-source-types)]
    {:selected is-activity-source-selected? :types feed-settings-for-types}))

(defn add-feed-settings-for-single-activity-source [activity-sources posted-parameters user-feed-settings activity-source-key]
  (let [single-activity-source-configuration (activity-source-key activity-sources)
        feed-settings-for-source (create-user-feed-settings-for-source activity-source-key single-activity-source-configuration posted-parameters)]
    (assoc user-feed-settings activity-source-key feed-settings-for-source)))

(defn create-user-feed-settings [activity-sources posted-parameters]
  (reduce (partial add-feed-settings-for-single-activity-source activity-sources posted-parameters)
          {}
          (keys activity-sources)))

(defn customise-feed [db request]
  (let [username (get-in request [:session :username])
        activity-sources (get-in request [:context :activity-sources])
        feed-settings-combined (create-user-feed-settings activity-sources (:params request))]
    (user/update-feed-settings! db username feed-settings-combined)
    (mh/redirect-to request :feed)))

(defn selected-feed? [a-feed-setting]
  (case a-feed-setting
    true true
    false false
    default-feed-selected-value))

(defn find-feed-preferences-for-activity-type [user-feed-activity-type-settings activity-type]
  (if-let [feed-preferences-for-activity-type (first (filter (fn[v] (= (:id v) activity-type)) user-feed-activity-type-settings))]
    feed-preferences-for-activity-type
    {:id activity-type :selected true}))

(defn generate-activity-type-preferences [avaiable-acitivty-type-from-source user-feed-activity-type-settings is-feed-selected?]
  (map (fn [v] {:name v
                :selected (and is-feed-selected? (:selected (find-feed-preferences-for-activity-type user-feed-activity-type-settings v)))})
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
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
  (a/retrieve-activities db active-activity-source-keys))

(defn map-over-values [m f]
  (into {} (for [[k v] m] [k (f v)])))

(defn type-settings->disabled-type-ids [types]
  (->> types (filter (complement :selected)) (map :id)))

(defn feed-settings->disabled-types-m [feed-settings]
  (map-over-values feed-settings
                   (comp set type-settings->disabled-type-ids :types)))

(defn feed-settings->disabled-sources [feed-settings]
  (->> feed-settings
       (filter #(= false (:selected (second %))))
       (map first)
       (into (set []))))

(defn generate-feed-query [feed-settings activity-sources]
  (let [disabled-types-m (feed-settings->disabled-types-m feed-settings)
        disabled-sources (feed-settings->disabled-sources feed-settings)]
    (for [[source description] activity-sources
          :when (not (contains? disabled-sources source))]
      (let [disabled-types-for-source (get disabled-types-m source)
            all-types-for-source (set (:activity-types description))]
        {"activity-src" (name source)
         "@type"        (into [] (clojure.set/difference all-types-for-source disabled-types-for-source))}))))

(defn feed [db request]
  (let [context (:context request)
        username (get-in request [:session :username])
        user (user/find-user db username)
        user-feed-settings (:feed-settings user)
        activity-sources (:activity-sources context)
        feed-query (generate-feed-query user-feed-settings activity-sources)
        active-activity-source-keys (get-active-activity-source-keys user-feed-settings activity-sources)
        activities (retrieve-activities-from-user-sources db feed-query)
        updated-context (assoc context :activities activities
                                       :active-activity-source-keys active-activity-source-keys)]
    (mh/enlive-response (f/feed (assoc request :context updated-context)) (:context request))))
(ns mooncake.controller.feed
  (:require [mooncake.activity :as a]
            [mooncake.db.user :as user]
            [mooncake.helper :as mh]
            [mooncake.controller.customise-feed :as cfc]
            [mooncake.view.feed :as f]
            [mooncake.config :as config]))

(defn activity-src-preferences->feed-query [preferences-for-an-activity-src]
  (let [selected-types (map :id (filter :selected (:activity-types preferences-for-an-activity-src)))]
    (when-not (empty? selected-types)
      {:activity-src     (name (:id preferences-for-an-activity-src))
       (keyword "@type") selected-types})))

(defn generate-feed-query [feed-settings activity-sources]
  (remove nil? (map activity-src-preferences->feed-query (cfc/generate-activity-source-preferences activity-sources feed-settings))))

(defn last-page-number [total-activities]
  (if (= total-activities 0)
    1
    (Math/ceil (/ total-activities config/activities-per-page))))

(defn parse-page-number [params]
  (try
    (Integer/parseInt (or (:page-number params) "1"))
    (catch Exception e
      nil)))

(defn page-number-is-in-correct-range [page-number total-activities]
  (when page-number
    (and (> page-number 0) (<= page-number (last-page-number total-activities)))))

(defn feed [store request]
  (let [context (:context request)
        params (:params request)
        username (get-in request [:session :username])
        user (user/find-user store username)
        user-feed-settings (:feed-settings user)
        activity-sources (:activity-sources context)
        feed-query (generate-feed-query user-feed-settings activity-sources)
        page-number (parse-page-number params)
        updated-params (-> params
                           (assoc :page-number page-number))
        activities (a/retrieve-activities store feed-query updated-params)
        total-activities (a/total-count-by-feed store feed-query)
        is-last-page (a/is-last-page? page-number total-activities)
        updated-context (-> context
                            (assoc :is-last-page is-last-page)
                            (assoc :activities activities))]

    (when (page-number-is-in-correct-range page-number total-activities)
      (mh/enlive-response (f/feed (assoc request :context updated-context :params updated-params)) request))))

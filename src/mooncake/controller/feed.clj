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
        params (:params request)
        username (get-in request [:session :username])
        user (user/find-user store username)
        user-feed-settings (:feed-settings user)
        activity-sources (:activity-sources context)
        feed-query (generate-feed-query user-feed-settings activity-sources)
        page-number (Integer/parseInt (or (:page-number params) "1"))
        updated-params (-> params
                           (assoc :page-number page-number))
        activities (a/retrieve-activities store feed-query updated-params)
        total-activities (a/total-count-by-feed store feed-query)
        is-last-page (a/is-last-page? page-number total-activities)
        updated-context (-> context
                            (assoc :is-last-page is-last-page)
                            (assoc :activities activities))]

    (mh/enlive-response (f/feed (assoc request :context updated-context :params updated-params)) request)))

(ns mooncake.controller.feed
  (:require [mooncake.activity :as a]
            [mooncake.db.user :as user]
            [mooncake.helper :as mh]
            [mooncake.controller.customise-feed :as cfc]
            [mooncake.view.feed :as f]
            [mooncake.db.activity :as dba]
            [ring.util.response :as r]))

(defn generate-feed-query [feed-settings activity-sources]
  (remove nil? (map a/activity-src-preferences->feed-query (cfc/generate-activity-source-preferences activity-sources feed-settings))))

(defn generate-user-feed-settings [store request]
  (let [username (get-in request [:session :username])
        user (user/find-user store username)]
    (:feed-settings user)))

(defn retrieve-activities [store request]
  (let [timestamp (get-in request [:params :timestamp])
        context (:context request)
        activity-sources (:activity-sources context)
        feed-query (generate-feed-query (generate-user-feed-settings store request) activity-sources)
        activities (dba/fetch-activities-by-timestamp store feed-query timestamp)
        jsonified-activities (a/activities->json activities request)
        response jsonified-activities]
    (-> (r/response response)
        (r/content-type "application/json"))))

(defn feed [store request]
  (let [params (:params request)
        context (:context request)
        activity-sources (:activity-sources context)
        page-number (a/parse-page-number (:page-number params))
        feed-query (generate-feed-query (generate-user-feed-settings store request) activity-sources)
        total-activities (a/total-count-by-feed store feed-query)
        is-last-page (a/is-last-page? page-number total-activities)
        updated-params (-> params
                           (assoc :page-number page-number))
        activities (a/retrieve-activities store feed-query updated-params)
        updated-context (-> context
                            (assoc :is-last-page is-last-page)
                            (assoc :activities activities))]
    (when (a/page-number-is-in-correct-range page-number total-activities)
      (mh/enlive-response (f/feed (assoc request :context updated-context :params updated-params)) request))))


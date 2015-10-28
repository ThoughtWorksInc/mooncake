(ns mooncake.controller.feed
  (:require [mooncake.activity :as a]
            [mooncake.db.user :as user]
            [mooncake.helper :as mh]
            [mooncake.controller.customise-feed :as cfc]
            [mooncake.view.feed :as f]
            [mooncake.config :as config]
            [mooncake.db.activity :as dba]
            [mooncake.domain.activity :as domain]
            [cheshire.core :as json]
            [clojure.tools.logging :as log]
            [ring.util.response :as r]
            [mooncake.view.view-helpers :as vh]
            [mooncake.translation :as t]))

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

(defn assoc-extra-information [activity-index-sources request activity]
  (let [action-text-key (domain/activity->action-text-key activity)
        translation-location (f/activity-action-message-translation action-text-key)
        translation (when translation-location (clojure.string/split translation-location #"/"))
        locale (t/get-locale-from-request request)
        action-text (if (empty? translation) (domain/activity->default-action-text activity)
                      (get-in request [:tconfig :dictionary (keyword locale) (keyword (first translation)) (keyword (last translation))]))]
    (-> activity
        (assoc :activity-src-no (get activity-index-sources (domain/activity->activity-src activity)))
        (assoc :formatted-time (mh/humanise-time (domain/activity->published activity)))
        (assoc :action-text action-text)
        (assoc :limited-title (vh/limit-text-length-if-above f/max-characters-in-title (domain/activity->object-display-name activity))))))

(defn activities->json [activities request]
  (let [activity-index-sources (f/index-activity-sources activities)
        updated-activities (map (partial assoc-extra-information activity-index-sources request) activities)]
    (json/generate-string {:activities updated-activities} {:pretty true})))

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
        jsonified-activities (activities->json activities request)
        response jsonified-activities]
    (-> (r/response response)
        (r/content-type "application/json"))))

(defn feed [store request]
  (let [params (:params request)
        context (:context request)
        activity-sources (:activity-sources context)
        page-number (parse-page-number params)
        feed-query (generate-feed-query (generate-user-feed-settings store request) activity-sources)
        total-activities (a/total-count-by-feed store feed-query)
        is-last-page (a/is-last-page? page-number total-activities)
        updated-params (-> params
                           (assoc :page-number page-number))
        activities (a/retrieve-activities store feed-query updated-params)
        updated-context (-> context
                            (assoc :is-last-page is-last-page)
                            (assoc :activities activities))]
    (when (page-number-is-in-correct-range page-number total-activities)
      (mh/enlive-response (f/feed (assoc request :context updated-context :params updated-params)) request))))


(ns mooncake.db.activity
  (:require [mooncake.domain.activity :as domain]
            [mooncake.db.mongo :as mongo]
            [clj-time.coerce :as time-coerce]
            [mooncake.config :as config])
  (:import org.bson.types.ObjectId))

(def activity-collection "activity")
(def activity-metadata-collection "activityMetaData")
(def relative-time (atom 0))

(defn fetch-activity-types [store]
  (let [all-activity-metadata (mongo/fetch-all store activity-metadata-collection)]
    (reduce
      (fn [activity-types-m activity-metadata]
        (assoc activity-types-m (:activity-src activity-metadata) (:activity-types activity-metadata)))
      {}
      all-activity-metadata)))

(defn update-activity-types-for-activity-source! [store activity-src activity-type]
  (mongo/add-to-set! store activity-metadata-collection {:activity-src activity-src} :activity-types activity-type))

(defn store-most-recent-activity-date! [store activity-src date]
  (mongo/upsert! store activity-metadata-collection {:activity-src activity-src} :latest-activity-datetime date))

(defn fetch-most-recent-activity-date [store activity-src]
  (when-let [item (mongo/find-item store activity-metadata-collection {:activity-src activity-src})]
    (-> item
        :latest-activity-datetime
        (time-coerce/from-string))))

(defn fetch-activities [store]
  (mongo/fetch-all store activity-collection))

(defn fetch-activities-by-timestamp-and-id [store activity-sources-and-types timestamp id older-items-requested?]
  (let [limit (when older-items-requested? config/activities-per-page)]
    (mongo/find-items-by-timestamp-and-id store activity-collection activity-sources-and-types {:sort {:published :descending} :limit limit} timestamp id older-items-requested?)))

(defn fetch-activities-by-activity-sources-and-types [store activity-sources-and-types params]
  (mongo/find-items-by-alternatives store activity-collection activity-sources-and-types (merge {:sort {:published :descending} :limit config/activities-per-page} params)))

(defn store-activity! [store activity]
  (let [activity-src (domain/activity->activity-src activity)
        activity-type (domain/activity->type activity)
        current-activity-date-string (domain/activity->published activity)
        id (ObjectId.)
        activity-with-relative-time (assoc activity :relInsertTime id :_id id)]
    (update-activity-types-for-activity-source! store activity-src activity-type)
    (store-most-recent-activity-date! store activity-src current-activity-date-string)
    (mongo/store! store activity-collection activity-with-relative-time)))

(defn fetch-total-count-by-sources-and-types [store activity-source-keys]
  (mongo/fetch-total-count-by-query store activity-collection activity-source-keys))

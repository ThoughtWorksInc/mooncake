(ns mooncake.db.activity
  (:require [mooncake.domain.activity :as domain]
            [mooncake.db.mongo :as mongo]
            [clj-time.coerce :as time-coerce]
            [clj-time.core :as time]))

(def activity-collection "activity")
(def activity-metadata-collection "activityMetaData")

(defn fetch-activity-types [db]
  (let [all-activity-metadata (mongo/fetch-all db activity-metadata-collection {:stringify? false})]
    (reduce
      (fn [activity-types-m activity-metadata]
        (assoc activity-types-m (:activity-src activity-metadata) (:activity-types activity-metadata)))
      {}
      all-activity-metadata)))

(defn update-activity-types-for-activity-source! [db activity-src activity-type]
  (mongo/add-to-set! db activity-metadata-collection {:activity-src activity-src} :activity-types activity-type))

(defn activity->published-datetime [activity]
  (-> (domain/activity->published activity)
      (time-coerce/from-string)))

(defn store-most-recent-activity-date! [db activity-src date]
  (mongo/upsert! db activity-metadata-collection {:activity-src activity-src} :latest-activity-datetime date))

(defn fetch-most-recent-activity-date [db activity-src]
  (when-let [item (mongo/find-item db activity-metadata-collection {:activity-src activity-src} {:stringify? false})]
    (-> item
        :latest-activity-datetime
        (time-coerce/from-string))))

(defn fetch-activities [db]
  (mongo/fetch-all db activity-collection {:stringify? true}))

(defn fetch-activities-by-activity-sources-and-types [db activity-sources-and-types]
  (mongo/find-items-by-alternatives db activity-collection activity-sources-and-types {:stringify? true :sort {"published" :descending} :limit 50}))

(defn store-activity! [db activity]
  (let [activity-src (domain/activity->activity-src activity)
        activity-type (domain/activity->type activity)
        most-recent-activity-date (fetch-most-recent-activity-date db activity-src)
        current-activity-date (activity->published-datetime activity)
        current-activity-date-string (domain/activity->published activity)]
    (when (or (not most-recent-activity-date) (time/after? current-activity-date most-recent-activity-date))
      (do
        (update-activity-types-for-activity-source! db activity-src activity-type)
        (store-most-recent-activity-date! db activity-src current-activity-date-string)
        (mongo/store! db activity-collection activity)))))

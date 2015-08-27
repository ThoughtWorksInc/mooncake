(ns mooncake.db.activity
  (:require [mooncake.domain.activity :as domain]
            [mooncake.db.mongo :as mongo]
            [clj-time.coerce :as time-coerce]
            [clj-time.core :as time]))

(def activity-collection "activity")
(def activity-metadata-collection "activityMetaData")

(defn activity->published-datetime [activity]
  (-> (domain/activity->published activity)
      (time-coerce/from-string)))

(defn get-most-recent-activity-date [activities]
  (->>
    activities
    (map activity->published-datetime)
    sort
    last))

(defn store-most-recent-activity-date! [db date]
  (mongo/upsert! db activity-metadata-collection {:name "latest-activity-datetime"} {:name "latest-activity-datetime"
                                                                                     :value date}))

(defn fetch-most-recent-activity-date [db]
  (when-let [item (mongo/find-item db activity-metadata-collection {:name "latest-activity-datetime"} true)]
    (-> item
        :value
        (time-coerce/from-string))))

(defn fetch-activities [db]
  (mongo/fetch-all db activity-collection false))

(defn store-activity! [db activity]
  (let [most-recent-activity-date (fetch-most-recent-activity-date db)
        current-activity-date (activity->published-datetime activity)
        current-activity-date-string (domain/activity->published activity)]
    (when (or (not most-recent-activity-date) (time/after? current-activity-date most-recent-activity-date))
      (do
        (store-most-recent-activity-date! db current-activity-date-string)
        (mongo/store! db activity-collection activity)))))
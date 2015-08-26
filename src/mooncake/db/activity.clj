(ns mooncake.db.activity
  (:require [mooncake.db.mongo :as mongo]
            [clj-time.coerce :as time-coerce]
            [clj-time.core :as time]))

(def collection "activity")

(defn activity->published-datetime [activity]
  (-> (get activity "published")
      (time-coerce/from-string)))

(defn get-most-recent-activity-date [activities]
  (->>
    activities
    (map activity->published-datetime)
    sort
    last))

(defn fetch-activities [db]
  (mongo/fetch-all db collection false))

(defn store-activity! [db activity]
  (let [activities (fetch-activities db)
        most-recent-activity-date (get-most-recent-activity-date activities)
        current-activity-date (activity->published-datetime activity)]
    (when (or (not most-recent-activity-date) (time/after? current-activity-date most-recent-activity-date))
      (mongo/store! db collection activity))))
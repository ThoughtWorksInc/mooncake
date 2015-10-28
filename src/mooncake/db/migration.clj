(ns mooncake.db.migration
  (:require [monger.ragtime]                                ;; monger.ragtime required for ragtime migrations to work
            [monger.collection :as mcoll]
            [ragtime.core :as ragtime]
            [clojure.tools.logging :as log]
            [mooncake.domain.activity :as activity]
            [mooncake.db.mongo :as mongo]
            [mooncake.db.activity :as adb]))

(defn add-activity-types-of-existing-activities-to-activity-src-metadata! [db]
  (log/info "Running migration add-activity-types-of-existing-activities-to-activity-src-metadata!")
  (let [store (mongo/create-mongo-store db)
        activities (adb/fetch-activities store)]
    (doseq [activity activities]
      (adb/update-activity-types-for-activity-source! store (activity/activity->activity-src activity) (activity/activity->type activity))))
  (log/info "Finished running migration add-activity-types-of-existing-activities-to-activity-src-metadata!"))

(defn add-signed-false-status-to-activities! [db]
  (log/info "Running migration add-signed-false-status-to-activities!")
  (let [store (mongo/create-mongo-store db)
        activities  (mcoll/find-maps db adb/activity-collection)]
    (doseq [activity activities]
      (when-not (:signed activity)
        (mongo/upsert! store adb/activity-collection {:_id (:_id activity)} :signed false))))
  (log/info "Finished running migration add-signed-false-status-to-activities!"))

;; IMPORTANT DO *NOT* MODIFY THE EXISTING MIGRATION IDS IN THIS LIST
(def migrations
  [{:id "add-activity-types-of-existing-activities-to-activity-src-metadata!"
    :up add-activity-types-of-existing-activities-to-activity-src-metadata!}
   {:id "add-signed-false-status-to-activities!"
    :up add-signed-false-status-to-activities!}])

(defn run-migrations
  ([db]
    (run-migrations db migrations))
  ([db migrations]
    (let [index (ragtime/into-index migrations)]
      (ragtime/migrate-all db index migrations))))


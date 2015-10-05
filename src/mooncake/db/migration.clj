(ns mooncake.db.migration
  (:require [monger.ragtime]                                ;; monger.ragtime required for ragtime migrations to work
            [ragtime.core :as ragtime]
            [clojure.tools.logging :as log]
            [mooncake.db.mongo :as mongo]
            [mooncake.db.activity :as adb]))


(defn add-activity-types-of-existing-activities-to-activity-src-metadata! [db]
  (log/info "Running migration add-activity-types-of-existing-activities-to-activity-src-metadata!")
  (let [database (mongo/create-database db)
        activities (adb/fetch-activities database)]
    (doseq [activity activities]
      (adb/update-activity-types-for-activity-source! database (get activity "activity-src") (get activity "@type"))))
  (log/info "Finished running migration add-activity-types-of-existing-activities-to-activity-src-metadata!"))

;; IMPORTANT DO *NOT* MODIFY THE EXISTING MIGRATION IDS IN THIS LIST
(def migrations
  [{:id "add-activity-types-of-existing-activities-to-activity-src-metadata!"
    :up add-activity-types-of-existing-activities-to-activity-src-metadata!}])

(defn run-migrations
  ([db]
    (run-migrations db migrations))
  ([db migrations]
    (let [index (ragtime/into-index migrations)]
      (ragtime/migrate-all db index migrations))))


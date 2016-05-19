(ns mooncake.integration.db.migration
  (:require [midje.sweet :refer :all]
            [monger.collection :as monger-c]
            [mooncake.db.mongo :as mongo]
            [mooncake.db.activity :as activity]
            [mooncake.test.test-helpers.db :as dbh]
            [mooncake.db.migration :as m]))

(defn test-migration-1 [db]
  (monger-c/insert db "fruit" {:name "orange"}))
(defn test-migration-2 [db]
  (monger-c/insert db "fruit" {:name "lemon"}))
(defn test-migration-3 [db]
  (monger-c/insert db "fruit" {:name "apple"}))

(defn count-fruit [db fruit-type]
  (monger-c/count db "fruit" {:name fruit-type}))

(def migrations [{:id "migration1"
                  :up test-migration-1}
                 {:id "migration2"
                  :up test-migration-2}])


(facts "About running migrations"
       (fact "each migration is only run once"
             (dbh/with-mongo-do
               (fn [db]
                 (m/run-migrations db migrations)
                 (m/run-migrations db migrations)
                 (count-fruit db "orange") => 1
                 (count-fruit db "lemon") => 1
                 (count-fruit db "apple") => 0)))

       (fact "can run a new migration"
             (dbh/with-mongo-do
               (fn [db]
                 (let [new-migration {:id "migration3" :up test-migration-3}
                       updated-migrations (conj migrations new-migration)]
                   (m/run-migrations db updated-migrations)
                   (count-fruit db "orange") => 1
                   (count-fruit db "lemon") => 1
                   (count-fruit db "apple") => 1)))))

(facts "About adding activity types to activity source metadata"
       (dbh/with-mongo-do
         (fn [db]
           (let [store (mongo/create-mongo-store db)]
             (mongo/store! store activity/activity-collection {:displayName      "KCat"
                                                               :published        "2015-08-12T10:20:41.000Z"
                                                               :activity-src     "source-1"
                                                               (keyword "@type") "Create"})
             (mongo/store! store activity/activity-collection {:displayName      "JDog"
                                                               :published        "2015-08-12T10:20:42.000Z"
                                                               :activity-src     "source-1"
                                                               (keyword "@type") "Question"})
             (mongo/store! store activity/activity-collection {:displayName      "LFrog"
                                                               :published        "2015-08-12T10:20:43.000Z"
                                                               :activity-src     "source-2"
                                                               (keyword "@type") "Create"})
             (activity/fetch-activity-types store) => {}
             (m/add-activity-types-of-existing-activities-to-activity-src-metadata! db)
             (activity/fetch-activity-types store) => {"source-1" ["Create" "Question"]
                                                       "source-2" ["Create"]}))))

(facts "About migration to add relative insertion id"
       (dbh/with-mongo-do
         (fn [db]
           (let [store (mongo/create-mongo-store db)
                 id1 1
                 id2 2
                 id3 3]
             (mongo/store-with-id! store activity/activity-collection :id {:id               id1
                                                                           :displayName      "KCat"
                                                                           :published        "2015-08-12T10:20:41.000Z"
                                                                           :activity-src     "source-1"
                                                                           (keyword "@type") "Create"})
             (mongo/store-with-id! store activity/activity-collection :id {:id               id2
                                                                           :displayName      "JDog"
                                                                           :published        "2015-08-12T10:20:42.000Z"
                                                                           :activity-src     "source-1"
                                                                           (keyword "@type") "Question"})
             (mongo/store-with-id! store activity/activity-collection :id {:id               id3
                                                                           :displayName      "LFrog"
                                                                           :published        "2015-08-12T10:20:43.000Z"
                                                                           :activity-src     "source-2"
                                                                           (keyword "@type") "Create"})
             (activity/fetch-activity-types store) => {}
             (m/make-activities-indexed-by-timestamp-and-id! db)
             (vec (activity/fetch-activities store)) => [{:displayName      "KCat"
                                                          :published        "2015-08-12T10:20:41.000Z"
                                                          :activity-src     "source-1"
                                                          (keyword "@type") "Create"
                                                          :id               id1
                                                          :relInsertTime    id1}
                                                         {:displayName      "JDog"
                                                          :published        "2015-08-12T10:20:42.000Z"
                                                          :activity-src     "source-1"
                                                          (keyword "@type") "Question"
                                                          :id               id2
                                                          :relInsertTime    id2}
                                                         {:displayName      "LFrog"
                                                          :published        "2015-08-12T10:20:43.000Z"
                                                          :activity-src     "source-2"
                                                          (keyword "@type") "Create"
                                                          :id               id3
                                                          :relInsertTime    id3}]
             (count (monger-c/indexes-on db "activity")) => 2))))

(facts "About adding :signed false status to activities that do not have :signed"
       (dbh/with-mongo-do
         (fn [db]
           (let [store (mongo/create-mongo-store db)
                 activity1-with-signed {:displayName      "KCat"
                                        :published        "2015-08-12T10:20:41.000Z"
                                        :activity-src     "source-1"
                                        (keyword "@type") "Create"
                                        :signed           true}
                 activity2-without-signed {:displayName      "JDog"
                                           :published        "2015-08-12T10:20:42.000Z"
                                           :activity-src     "source-1"
                                           (keyword "@type") "Question"}
                 activity2-with-signed {:displayName      "JDog"
                                        :published        "2015-08-12T10:20:42.000Z"
                                        :activity-src     "source-1"
                                        (keyword "@type") "Question"
                                        :signed           false}]
             (activity/fetch-activities store) => ()
             (mongo/store! store activity/activity-collection activity1-with-signed)
             (mongo/store! store activity/activity-collection activity2-without-signed)
             (activity/fetch-activities store) => [activity1-with-signed activity2-without-signed]
             (m/add-signed-false-status-to-activities! db)
             (activity/fetch-activities store) => [activity1-with-signed activity2-with-signed]))))


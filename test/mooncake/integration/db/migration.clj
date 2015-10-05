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
           (let [database (mongo/create-database db)]
             (mongo/store! database activity/activity-collection {"@displayName" "KCat"
                                                                  "published"    "2015-08-12T10:20:41.000Z"
                                                                  "activity-src" "source-1"
                                                                  "@type"        "Create"})
             (mongo/store! database activity/activity-collection {"@displayName" "JDog"
                                                                  "published"    "2015-08-12T10:20:42.000Z"
                                                                  "activity-src" "source-1"
                                                                  "@type"        "Question"})
             (mongo/store! database activity/activity-collection {"@displayName" "LFrog"
                                                                  "published"    "2015-08-12T10:20:43.000Z"
                                                                  "activity-src" "source-2"
                                                                  "@type"        "Create"})
             (activity/fetch-activity-types database) => {}
             (m/add-activity-types-of-existing-activities-to-activity-src-metadata! db)
             (activity/fetch-activity-types database) => {"source-1" ["Create" "Question"]
                                                          "source-2" ["Create"]}))))


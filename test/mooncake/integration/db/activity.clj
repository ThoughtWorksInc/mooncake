(ns mooncake.integration.db.activity
  (:require [midje.sweet :refer :all]
            [clj-time.core :as time]
            [mooncake.db.activity :as activity]
            [mooncake.test.test-helpers.db :as dbh]
            [mooncake.db.mongo :as mongo]))

(background (before :facts (dbh/drop-db!)))

(fact "can store an activity"
      (dbh/with-mongo-do
        (fn [db]
          (let [database (mongo/create-database db)
                activity {"@displayName" "KCat"
                          "published" "2015-08-12T10:20:41.000Z"}]
            (activity/store-activity! database activity)
            (mongo/find-item database activity/collection {"@displayName" "KCat"} false) => activity))))

(fact "can retrieve most recent activity date from activities"
      (let [activities [{"published" "2015-08-12T10:20:41.000Z"}
                        {"published" "2015-08-12T10:20:43.000Z"}
                        {"published" "2015-08-12T10:20:40.000Z"}
                        {"published" "2015-07-12T11:21:40.000Z"}]]
        (activity/get-most-recent-activity-date activities) => (time/date-time 2015 8 12 10 20 43 0)))

(fact "will not store the existing activity"
      (dbh/with-mongo-do
        (fn [db]
          (let [database (mongo/create-database db)
                activity {"published" "2015-08-12T10:20:41.369Z"
                          "@displayName" "KCat"}]
            (activity/store-activity! database activity)
            (count (activity/fetch-activities database)) => 1
            (activity/store-activity! database activity)
            (count (activity/fetch-activities database)) => 1))))

(fact "can fetch a collection of activities"
      (dbh/with-mongo-do
        (fn [db]
          (let [database (mongo/create-database db)
                activity {"@displayName" "KCat"
                          "published" "2015-08-12T10:20:41.000Z"}]
            (activity/store-activity! database activity)
            (activity/fetch-activities database) => [activity]))))
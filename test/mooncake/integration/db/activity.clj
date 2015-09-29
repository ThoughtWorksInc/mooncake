(ns mooncake.integration.db.activity
  (:require [midje.sweet :refer :all]
            [clj-time.core :as time]
            [mooncake.db.activity :as activity]
            [mooncake.test.test-helpers.db :as dbh]
            [mooncake.db.mongo :as mongo]))

(fact "can store an activity"
      (dbh/with-mongo-do
        (fn [db]
          (let [database (mongo/create-database db)
                activity {"@displayName" "KCat"
                          "published" "2015-08-12T10:20:41.000Z"}]
            (activity/store-activity! database activity)
            (mongo/find-item database activity/activity-collection {"@displayName" "KCat"} {:stringify? true}) => activity))))

(fact "will not store the existing activity"
      (dbh/with-mongo-do
        (fn [db]
          (let [database (mongo/create-database db)
                activity1 {"published" "2015-08-12T10:20:41.369Z"
                          "@displayName" "KCat"}
                activity2 {"published" "2015-08-12T10:20:40.000"
                           "@displayName" "JDog"}]
            (activity/store-activity! database activity1)
            (activity/fetch-activities database) => [activity1]
            (fact "does not add activity because timestamp is not after latest timestamp"
                  (activity/store-activity! database activity2)
                  (activity/fetch-activities database) => [activity1])))))

(fact "can fetch a collection of activities"
      (dbh/with-mongo-do
        (fn [db]
          (let [database (mongo/create-database db)
                activity {"@displayName" "KCat"
                          "published" "2015-08-12T10:20:41.000Z"}]
            (activity/store-activity! database activity)
            (activity/fetch-activities database) => [activity]))))

(fact "can store and retrieve latest-activity-time in metadata collection per activity-src"
      (dbh/with-mongo-do
        (fn [db]
          (let [database (mongo/create-database db)
                api1 "api1"
                api2 "api2"]
            (activity/fetch-most-recent-activity-date database api1) => nil
            (activity/fetch-most-recent-activity-date database api2) => nil

            (activity/store-most-recent-activity-date! database api1 "2015-08-12T10:20:41.000Z")
            (activity/fetch-most-recent-activity-date database api1) => (time/date-time 2015 8 12 10 20 41 0)
            (activity/fetch-most-recent-activity-date database api2) => nil

            (activity/store-most-recent-activity-date! database api1 "2015-08-12T10:20:42.000Z")
            (activity/store-most-recent-activity-date! database api2 "2015-08-12T10:20:43.000Z")
            (activity/fetch-most-recent-activity-date database api1) => (time/date-time 2015 8 12 10 20 42 0)
            (activity/fetch-most-recent-activity-date database api2) => (time/date-time 2015 8 12 10 20 43 0)))))

(fact "if new event is retrieved from API1 that is has an older timestamp than existing event from API2, then event is still stored"
      (dbh/with-mongo-do
        (fn [db]
          (let [database (mongo/create-database db)
                event1api1 {"@displayName" "KCat"
                            "published" "2015-08-12T10:20:41.000Z"
                            "activity-src" "api1"}
                event2api2 {"@diplayName" "JDog"
                            "published" "2015-08-12T10:20:41.000Z"
                            "activity-src" "api2"}]
            (activity/store-activity! database event1api1)
            ;; time passes
            (activity/store-activity! database event2api2)
            (count (activity/fetch-activities database)) => 2))))


(tabular
  (fact "can fetch a collection of activities with the given activity source keys and activity types"
        (dbh/with-mongo-do
          (fn [db]
            (let [database (mongo/create-database db)
                  activity1 {"@displayName" "KCat"
                             "published" "2015-08-12T10:20:41.000Z"
                             "activity-src" "source-1"
                             "@type" "Create"}
                  activity2 {"@displayName" "KCat"
                             "published" "2015-08-12T10:20:42.000Z"
                             "activity-src" "source-1"
                             "@type" "Question"}
                  activity3 {"@displayName" "JDon"
                             "published" "2015-08-12T11:20:41.000Z"
                             "activity-src" "source-2"
                             "@type" "Create"}]
              (activity/store-activity! database activity1)
              (activity/store-activity! database activity2)
              (activity/store-activity! database activity3)
              (activity/fetch-activities-by-activity-sources-and-types database ?activity-sources-and-types) => ?result))))

  ?activity-sources-and-types                                           ?result
  [{:activity-src :source-1 "@type" ["Create"]}]                        [activity1]
  [{:activity-src :source-1 "@type" ["Create" "Question"]}]             [activity2 activity1]
  [{:activity-src :source-1 "@type" ["Create" "Question"]}
   {:activity-src :source-2 "@type" ["Create" "Add"]}]                  [activity3 activity2 activity1]
  []                                                                    []
  nil                                                                   [])

(fact "activities are fetched in batches of 50"
      (dbh/with-mongo-do
        (fn [db]
          (let [database (mongo/create-database db)]
            (->> (range 51)
                 (map (fn [counter]
                        {"@displayName" (str "KCat" counter)
                         "published"    (format "2015-08-12T10:20:%02d.000Z" counter)
                         "activity-src" "source-1"
                         "@type"        "Create"}))
                 (map (partial activity/store-activity! database))
                 doall)
            (activity/fetch-activities-by-activity-sources-and-types database [{:activity-src :source-1 "@type" ["Create"]}]) => (n-of anything 50)))))

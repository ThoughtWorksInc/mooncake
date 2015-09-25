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
                stored-activity {"@displayName" "KCat"
                                 "published"    "2015-08-12T10:20:41.000Z"}
                retrieved-activity {(keyword "@displayName") "KCat"
                                    :published               "2015-08-12T10:20:41.000Z"}]
            (activity/store-activity! database stored-activity)
            (mongo/find-item database activity/activity-collection {(keyword "@displayName") "KCat"}) => retrieved-activity))))


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
                stored-activity-one {"published"    "2015-08-12T10:20:41.369Z"
                                     "@displayName" "KCat"}
                stored-activity-two {"published"    "2015-08-12T10:20:40.000"
                                     "@displayName" "JDog"}
                fetched-activity-one {:published               "2015-08-12T10:20:41.369Z"
                                      (keyword "@displayName") "KCat"}]
            (activity/store-activity! database stored-activity-one)
            (activity/fetch-activities database) => [fetched-activity-one]
            (fact "does not add activity because timestamp is not after latest timestamp"
                  (activity/store-activity! database stored-activity-two)
                  (activity/fetch-activities database) => [fetched-activity-one])))))


(fact "can fetch a collection of activities"
      (dbh/with-mongo-do
        (fn [db]
          (let [database (mongo/create-database db)
                stored-activity {"@displayName" "KCat"
                                 "published"    "2015-08-12T10:20:41.000Z"}
                fetched-activity {(keyword "@displayName") "KCat"
                                  :published               "2015-08-12T10:20:41.000Z"}]
            (activity/store-activity! database stored-activity)
            (activity/fetch-activities database) => [fetched-activity]))))

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

(fact "if new event is retrieved from API1 that has an older timestamp than existing event from API2, then event is still stored"
      (dbh/with-mongo-do
        (fn [db]
          (let [database (mongo/create-database db)
                event1api1 {"@displayName" "KCat"
                            "published"    "2015-08-12T10:20:41.000Z"
                            "activity-src" "api1"}
                event2api2 {"@diplayName"  "JDog"
                            "published"    "2015-08-12T10:20:41.000Z"
                            "activity-src" "api2"}]
            (activity/store-activity! database event1api1)
            ;; time passes
            (activity/store-activity! database event2api2)
            (count (activity/fetch-activities database)) => 2))))


(tabular
  (fact "can fetch a collection of activities with the given activity source keys"
        (dbh/with-mongo-do
          (fn [db]
            (let [database (mongo/create-database db)
                  stored-activity-one {"@displayName" "KCat"
                                       "published"    "2015-08-12T10:20:41.000Z"
                                       "activity-src" "source-1"}
                  stored-activity-two {"@displayName" "JDon"
                                       "published"    "2015-08-12T11:20:41.000Z"
                                       "activity-src" "source-2"}
                  retrieved-activity-one {(keyword "@displayName") "KCat"
                                          :published               "2015-08-12T10:20:41.000Z"
                                          :activity-src            "source-1"}
                  retrieved-activity-two {(keyword "@displayName") "JDon"
                                          :published               "2015-08-12T11:20:41.000Z"
                                          :activity-src            "source-2"}]
              (activity/store-activity! database stored-activity-one)
              (activity/store-activity! database stored-activity-two)
              (activity/fetch-activities-by-activity-source database ?activity-source-keys) => ?result))))

  ?activity-source-keys              ?result
  [:source-1 :source-2]              [retrieved-activity-one retrieved-activity-two]
  [:source-1]                        [retrieved-activity-one]
  [:source-1 :source-2 :source-x]    [retrieved-activity-one retrieved-activity-two]
  [:source-x]                        []
  []                                 []
  nil                                [])


(tabular
  (fact "can fetch a collection of activities with the given activity source keys and activity types"
        (dbh/with-mongo-do
          (fn [db]
            (let [database (mongo/create-database db)
                  stored-activity-one {"@displayName" "KCat"
                                       "published"    "2015-08-12T10:20:41.000Z"
                                       "activity-src" "source-1"
                                       "@type"        "Create"}
                  stored-activity-two {"@displayName" "KCat"
                                       "published"    "2015-08-12T10:20:42.000Z"
                                       "activity-src" "source-1"
                                       "@type"        "Question"}
                  stored-activity-three {"@displayName" "JDon"
                                         "published"    "2015-08-12T11:20:41.000Z"
                                         "activity-src" "source-2"
                                         "@type"        "Create"}
                  retrieved-activity-one {(keyword "@displayName") "KCat"
                                          :published               "2015-08-12T10:20:41.000Z"
                                          :activity-src            "source-1"
                                          (keyword "@type")        "Create"}
                  retrieved-activity-two {(keyword "@displayName") "KCat"
                                          :published               "2015-08-12T10:20:42.000Z"
                                          :activity-src            "source-1"
                                          (keyword "@type")        "Question"}
                  retrieved-activity-three {(keyword "@displayName") "JDon"
                                            :published               "2015-08-12T11:20:41.000Z"
                                            :activity-src            "source-2"
                                            (keyword "@type")        "Create"}]
              (activity/store-activity! database stored-activity-one)
              (activity/store-activity! database stored-activity-two)
              (activity/store-activity! database stored-activity-three)
              (activity/fetch-activities-by-activity-sources-and-types database ?activity-sources-and-types) => ?result))))

  ?activity-sources-and-types                                          ?result
  [{:activity-src :source-1 (keyword "@type") ["Create"]}]             [retrieved-activity-one]
  [{:activity-src :source-1 (keyword "@type") ["Create" "Question"]}]  [retrieved-activity-one retrieved-activity-two]
  [{:activity-src :source-1 (keyword "@type") ["Create" "Question"]}
   {:activity-src :source-2 (keyword "@type") ["Create" "Add"]}]       [retrieved-activity-one retrieved-activity-two retrieved-activity-three]
  []                                                                   []
  nil                                                                  [])

(ns mooncake.integration.db.activity
  (:require [midje.sweet :refer :all]
            [clj-time.core :as time]
            [mooncake.db.activity :as activity]
            [mooncake.test.test-helpers.db :as dbh]
            [mooncake.db.mongo :as mongo]
            [mooncake.config :as config]))

(fact "can store an activity"
      (dbh/with-mongo-do
        (fn [db]
          (let [store (mongo/create-mongo-store db)
                activity {:displayName "KCat"
                          :published "2015-08-12T10:20:41.000Z"
                          :signed "verification-failed"}]
            (activity/store-activity! store activity)
            (mongo/find-item store activity/activity-collection {:displayName "KCat"}) => activity))))

(fact "will not store the existing activity"
      (dbh/with-mongo-do
        (fn [db]
          (let [store (mongo/create-mongo-store db)
                activity1 {:published "2015-08-12T10:20:41.369Z"
                          :displayName "KCat"}
                activity2 {:published "2015-08-12T10:20:40.000"
                           :displayName "JDog"}]
            (activity/store-activity! store activity1)
            (activity/fetch-activities store) => [activity1]
            (fact "does not add activity because timestamp is not after latest timestamp"
                  (activity/store-activity! store activity2)
                  (activity/fetch-activities store) => [activity1])))))

(fact "can fetch a collection of activities"
      (dbh/with-mongo-do
        (fn [db]
          (let [store (mongo/create-mongo-store db)
                activity {:displayName "KCat"
                          :published "2015-08-12T10:20:41.000Z"}]
            (activity/store-activity! store activity)
            (activity/fetch-activities store) => [activity]))))

(fact "can store and retrieve latest-activity-time in metadata collection per activity-src"
      (dbh/with-mongo-do
        (fn [db]
          (let [store (mongo/create-mongo-store db)
                api1 "api1"
                api2 "api2"]
            (activity/fetch-most-recent-activity-date store api1) => nil
            (activity/fetch-most-recent-activity-date store api2) => nil

            (activity/store-most-recent-activity-date! store api1 "2015-08-12T10:20:41.000Z")
            (activity/fetch-most-recent-activity-date store api1) => (time/date-time 2015 8 12 10 20 41 0)
            (activity/fetch-most-recent-activity-date store api2) => nil

            (activity/store-most-recent-activity-date! store api1 "2015-08-12T10:20:42.000Z")
            (activity/store-most-recent-activity-date! store api2 "2015-08-12T10:20:43.000Z")
            (activity/fetch-most-recent-activity-date store api1) => (time/date-time 2015 8 12 10 20 42 0)
            (activity/fetch-most-recent-activity-date store api2) => (time/date-time 2015 8 12 10 20 43 0)))))

(fact "can store and retrieve activity-types in metadata collection per activity-src"
      (dbh/with-mongo-do
        (fn [db]
          (let [store (mongo/create-mongo-store db)
                activity-src "an-activity-src"]
            (activity/fetch-activity-types store) => {}
            (activity/update-activity-types-for-activity-source! store activity-src "Type1")
            (activity/fetch-activity-types store) => {"an-activity-src" ["Type1"]}

            (fact "can store multiple activity-types"
                  (activity/update-activity-types-for-activity-source! store activity-src "Type2")
                  (activity/fetch-activity-types store) => {"an-activity-src" ["Type1" "Type2"]})

            (fact "will not store duplicates"
                  (activity/fetch-activity-types store) => {"an-activity-src" ["Type1" "Type2"]}
                  (activity/update-activity-types-for-activity-source! store activity-src "Type2")
                  (activity/fetch-activity-types store) => {"an-activity-src" ["Type1" "Type2"]})))))


(fact "if new event is retrieved from API1 that is has an older timestamp than existing event from API2, then event is still stored"
      (dbh/with-mongo-do
        (fn [db]
          (let [store (mongo/create-mongo-store db)
                event1api1 {:displayName "KCat"
                            :published "2015-08-12T10:20:41.000Z"
                            :activity-src "api1"}
                event2api2 {:displayName "JDog"
                            :published "2015-08-12T10:20:41.000Z"
                            :activity-src "api2"}]
            (activity/store-activity! store event1api1)
            ;; time passes
            (activity/store-activity! store event2api2)
            (count (activity/fetch-activities store)) => 2))))

(tabular
  (fact "can fetch a collection of activities with the given activity source keys and activity types"
        (dbh/with-mongo-do
          (fn [db]
            (let [store (mongo/create-mongo-store db)
                  activity1 {:displayName "KCat"
                             :published "2015-08-12T10:20:41.000Z"
                             :activity-src "source-1"
                             (keyword "@type") "Create"}
                  activity2 {:displayName "KCat"
                             :published "2015-08-12T10:20:42.000Z"
                             :activity-src "source-1"
                             (keyword "@type") "Question"}
                  activity3 {:displayName "JDon"
                             :published "2015-08-12T11:20:41.000Z"
                             :activity-src "source-2"
                             (keyword "@type") "Create"}]
              (activity/store-activity! store activity1)
              (activity/store-activity! store activity2)
              (activity/store-activity! store activity3)
              (activity/fetch-activities-by-activity-sources-and-types store ?activity-sources-and-types {}) => ?result))))

  ?activity-sources-and-types                                                     ?result
  [{:activity-src :source-1 (keyword "@type") ["Create"]}]                        [activity1]
  [{:activity-src :source-1 (keyword "@type") ["Create" "Question"]}]             [activity2 activity1]
  [{:activity-src :source-1 (keyword "@type") ["Create" "Question"]}
   {:activity-src :source-2 (keyword "@type") ["Create" "Add"]}]                  [activity3 activity2 activity1]
  []                                                                              []
  nil                                                                             [])

(facts "fetching activities with query"
       (dbh/with-mongo-do
         (fn [db]
           (let [store (mongo/create-mongo-store db)]
             (dbh/create-dummy-activities store (+ 1 config/activities-per-page))
             (fact "activities are fetched in batches"
                   (activity/fetch-activities-by-activity-sources-and-types store [{:activity-src :test-source (keyword "@type") ["Create"]}] {}) => (n-of anything config/activities-per-page))

             (fact "activities are paginated with correct batch amount per page"
                   (activity/fetch-activities-by-activity-sources-and-types store [{:activity-src :test-source (keyword "@type") ["Create"]}] {:page-number 2}) => (one-of anything))))))

(let [latest-time "2015-08-12T00:00:02.000Z"
      second-latest-time "2015-08-12T00:00:01.000Z"
      oldest-time "2015-08-12T00:00:00.000Z"]
  (tabular
    (fact "fetching activities with timestamp"
          (dbh/with-mongo-do
            (fn [db]
              (let [
                    store (mongo/create-mongo-store db)
                    activity1 {:displayName      "KCat"
                               :published        oldest-time
                               :activity-src     "source-1"
                               (keyword "@type") "Create"}
                    activity2 {:displayName      "KCat"
                               :published        second-latest-time
                               :activity-src     "source-1"
                               (keyword "@type") "Create"}
                    activity3 {:displayName      "JDon"
                               :published        latest-time
                               :activity-src     "source-1"
                               (keyword "@type") "Create"}]
                (activity/store-activity! store activity1)
                (activity/store-activity! store activity2)
                (activity/store-activity! store activity3)
                (activity/fetch-activities-by-timestamp store [{:activity-src :source-1 (keyword "@type") ["Create"]}] ?timestamp ?older-items-requested) => ?result))))
    ?older-items-requested ?timestamp            ?result
    true                    oldest-time           []
    true                    second-latest-time    [activity1]
    true                    latest-time           [activity2
                                                   activity1]))

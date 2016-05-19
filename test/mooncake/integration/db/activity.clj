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
                          :published   "2015-08-12T10:20:41.000Z"
                          :signed      "verification-failed"}
                insert-time (:relInsertTime (activity/store-activity! store activity))]

            (mongo/find-item store activity/activity-collection {:displayName "KCat"}) => (assoc activity :relInsertTime insert-time)))))

(facts "about activity relative time generation"
       (fact "creation of an activity generates a relative time id"
             (dbh/with-mongo-do
               (fn [db]
                 (let [
                       store (mongo/create-mongo-store db)
                       activity1 {:displayName  "KCat"
                                  :published    "2014-08-12T00:00:00.000Z"
                                  :activity-src "test-source"
                                  :type         "Create"}
                       activity2 {:displayName  "JDog"
                                  :published    "2014-08-12T00:00:00.000Z"
                                  :activity-src "test-source"
                                  :type         "Create"}
                       activity3 {:displayName  "HFish"
                                  :published    "2014-08-12T00:00:00.000Z"
                                  :activity-src "test-source"
                                  :type         "Create"}
                       id1 (str (:relInsertTime (activity/store-activity! store activity1)))
                       id2 (str (:relInsertTime (activity/store-activity! store activity2)))
                       id3 (str (:relInsertTime (activity/store-activity! store activity3)))
                       ]
                   id1 =not=> nil
                   (< (compare id1 id2) 0) => true
                   (< (compare id2 id3) 0) => true)))))

(fact "can fetch a collection of activities"
      (dbh/with-mongo-do
        (fn [db]
          (let [store (mongo/create-mongo-store db)
                activity {:displayName "KCat"
                          :published   "2015-08-12T10:20:41.000Z"}
                insert-time (:relInsertTime (activity/store-activity! store activity))]
            (activity/fetch-activities store) => [(assoc activity :relInsertTime insert-time)]))))

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
                event1api1 {:displayName  "KCat"
                            :published    "2015-08-12T10:20:41.000Z"
                            :activity-src "api1"}
                event2api2 {:displayName  "JDog"
                            :published    "2015-08-12T10:20:41.000Z"
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
                  activity1 {:displayName  "KCat"
                             :published    "2015-08-12T10:20:41.000Z"
                             :activity-src "source-1"
                             :type         "Create"}
                  activity2 {:displayName  "KCat"
                             :published    "2015-08-12T10:20:42.000Z"
                             :activity-src "source-1"
                             :type         "Question"}
                  activity3 {:displayName  "JDon"
                             :published    "2015-08-12T11:20:41.000Z"
                             :activity-src "source-2"
                             :type         "Create"}
                  id1 (:relInsertTime (activity/store-activity! store activity1))
                  id2 (:relInsertTime (activity/store-activity! store activity2))
                  id3 (:relInsertTime (activity/store-activity! store activity3))]
              (activity/fetch-activities-by-activity-sources-and-types store ?activity-sources-and-types {}) => ?result))))

  ?activity-sources-and-types                                         ?result
  [{:activity-src :source-1 :type ["Create"]}]                        [(assoc activity1 :relInsertTime id1)]
  [{:activity-src :source-1 :type ["Create" "Question"]}]             [(assoc activity2 :relInsertTime id2) (assoc activity1 :relInsertTime id1)]
  [{:activity-src :source-1 :type ["Create" "Question"]}
   {:activity-src :source-2 :type ["Create" "Add"]}]                  [(assoc activity3 :relInsertTime id3) (assoc activity2 :relInsertTime id2) (assoc activity1 :relInsertTime id1)]
  []                                                                  []
  nil                                                                 [])

(facts "fetching activities with query"
       (dbh/with-mongo-do
         (fn [db]
           (let [store (mongo/create-mongo-store db)]
             (dbh/create-dummy-activities store (+ 1 config/activities-per-page))
             (fact "activities are fetched in batches"
                   (activity/fetch-activities-by-activity-sources-and-types store [{:activity-src :test-source :type ["Create"]}] {}) => (n-of anything config/activities-per-page))

             (fact "activities are paginated with correct batch amount per page"
                   (activity/fetch-activities-by-activity-sources-and-types store [{:activity-src :test-source :type ["Create"]}] {:page-number 2}) => (one-of anything))))))

(let [latest-time "2016-08-12T00:00:02.000Z"
      second-latest-time "2016-08-12T00:00:01.000Z"
      second-oldest-time "2014-08-13T00:00:00.000Z"
      oldest-time "2014-08-12T00:00:00.000Z"]
  (dbh/with-mongo-do
    (fn [db]
      (let [store (mongo/create-mongo-store db)
            activity1 {:displayName      "KCat"
                       :published        oldest-time
                       :activity-src     "test-source"
                       :type "Create"}
            activity2 {:displayName      "JDog"
                       :published        second-latest-time
                       :activity-src     "test-source"
                       :type "Create"}
            activity3 {:displayName  "HFish"
                       :published    latest-time
                       :activity-src "test-source"
                       :type         "Create"}
            activity4 {:name         "GBird"
                       :published    second-oldest-time
                       :activity-src "test-source-2"
                       :type         "Question"}
            id1 (:relInsertTime (activity/store-activity! store activity1))
            id2 (:relInsertTime (activity/store-activity! store activity2))
            id3 (:relInsertTime (activity/store-activity! store activity3))
            id4 (:relInsertTime (activity/store-activity! store activity4))
            _ (dbh/create-dummy-activities store config/activities-per-page)]

        (tabular
          (fact "fetching activities with timestamp"
                (count (activity/fetch-activities-by-timestamp-and-id store
                                                                      [{:activity-src :test-source :type ["Create"]}
                                                                       {:activity-src :test-source :type ["Create"]}
                                                                       {:activity-src :test-source-2 :type ["Question"]}]
                                                                      ?timestamp ?insert-time-id ?older-items-requested)) => ?result)
          ?older-items-requested ?timestamp         ?result                          ?insert-time-id
          true                   oldest-time        0                                id1
          true                   oldest-time        1                                id4
          true                   second-latest-time config/activities-per-page       id2
          true                   latest-time        config/activities-per-page       id3
          false                  oldest-time        (+ 3 config/activities-per-page) id1
          false                  second-latest-time 1                                id2
          false                  latest-time        0                                id3))))

  (facts "ordering activities with identical timestamps"
         (fact "requests will return other objects with the same timestamp if they are later in ordering"
               (dbh/with-mongo-do
                 (fn [db]
                   (let [
                         store (mongo/create-mongo-store db)
                         activity1 {:displayName      "KCat"
                                    :published        oldest-time
                                    :activity-src     "test-source"
                                    :type "Create"}
                         activity2 {:displayName      "JDog"
                                    :published        oldest-time
                                    :activity-src     "test-source"
                                    :type "Create"}
                         activity3 {:displayName      "HFish"
                                    :published        oldest-time
                                    :activity-src     "test-source"
                                    :type "Create"}
                         id (:relInsertTime (activity/store-activity! store activity1))]
                     (activity/store-activity! store activity2)
                     (activity/store-activity! store activity3)
                     (count
                       (activity/fetch-activities-by-timestamp-and-id store [
                                                                             {:activity-src :test-source :type ["Create"]}
                                                                             ] oldest-time id false)) => 2))))))

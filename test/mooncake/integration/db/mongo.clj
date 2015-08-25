(ns mooncake.integration.db.mongo
  (:require
    [midje.sweet :refer :all]
    [monger.collection :as c]
    [mooncake.db.mongo :as mongo]
    [mooncake.test.test-helpers.db :as dbh]))

(def collection-name "stuff")

(background (before :facts (dbh/drop-db!)))

(fact "creating mongo store from mongo uri creates a MongoDatabase which can be used to store-with-id! and fetch"
      (dbh/with-mongo-do
        (fn [mongo-db]
          (let [store (mongo/create-database mongo-db)]
            (class store) => mooncake.db.mongo.MongoDatabase
            (mongo/store-with-id! store collection-name :some-index-key {:some-index-key "barry" :some-other-key "other"})
            (mongo/fetch store collection-name "barry") => {:some-index-key "barry" :some-other-key "other"}))))

(fact "storing an item in an empty collection results in just that item being in the collection"
      (dbh/with-mongo-do
        (fn [mongo-db]
          (let [store (mongo/create-database mongo-db)
                item {:some-index-key "barry" :some-other-key "other"}]
            (c/find-maps mongo-db collection-name) => empty?
            (mongo/store-with-id! store collection-name :some-index-key item) => item
            (count (c/find-maps mongo-db collection-name)) => 1
            (c/find-one-as-map mongo-db collection-name {:some-index-key "barry"}) => (contains item)))))

(fact "storing an item with a duplicate key throws an exception"
      (dbh/with-mongo-do
        (fn [mongo-db]
          (let [store (mongo/create-database mongo-db)
                item {:some-index-key "barry" :some-other-key "other"}]
            (mongo/store-with-id! store collection-name :some-index-key item) => item
            (mongo/store-with-id! store collection-name :some-index-key item) => (throws Exception)))))

(fact "find-item queries items based on a query map and returns one if a match is found"
      (dbh/with-mongo-do
        (fn [mongo-db]
          (let [store (mongo/create-database mongo-db)
                item1 {:some-index-key "barry" :some-other-key "other"}
                item2 {:some-index-key "rebecca" :some-other-key "bsaa"}
                item3 {:some-index-key "zane"    :some-other-key "foo" :a-third-key "bar"}
                _ (mongo/store-with-id! store collection-name :some-index-key item1)
                _ (mongo/store-with-id! store collection-name :some-index-key item2)
                _ (mongo/store-with-id! store collection-name :some-index-key item3)]
            (mongo/find-item store collection-name {:some-other-key "other"}) => item1
            (mongo/find-item store collection-name {:some-other-key "bsaa"}) => item2
            (mongo/find-item store collection-name {:some-other-key "foo" :a-third-key "bar"}) => item3
            (mongo/find-item store collection-name {:some-other-key "nonExisty"}) => nil
            (mongo/find-item store collection-name nil) => nil))))

(ns mooncake.integration.db.mongo
  (:require
    [midje.sweet :refer :all]
    [monger.core :as m]
    [monger.collection :as c]
    [monger.db :as mdb]
    [mooncake.db.mongo :as mongo]))

(def test-db "mooncake-test")
(def test-db-uri (str "mongodb://localhost:27017/" test-db))
(def collection-name "stuff")

(defn with-mongo-do [mongo-uri thing-to-do]
  (let [{:keys [db conn]} (mongo/get-mongo-db-and-conn mongo-uri)]
    (try (thing-to-do db)
         (finally (m/disconnect conn)))))

(defn drop-db! []
  (let [{:keys [conn db]} (m/connect-via-uri test-db-uri)]
    (mdb/drop-db db)
    (m/disconnect conn)))

(background (before :facts (drop-db!)))

(fact "creating mongo store from mongo uri creates a MongoDatabase which can be used to store! and fetch"
      (with-mongo-do test-db-uri
        (fn [mongo-db]
          (let [store (mongo/create-database mongo-db)]
            (class store) => mooncake.db.mongo.MongoDatabase
            (mongo/store! store collection-name :some-index-key {:some-index-key "barry" :some-other-key "other"})
            (mongo/fetch store collection-name "barry") => {:some-index-key "barry" :some-other-key "other"}))))

(fact "storing an item in an empty collection results in just that item being in the collection"
      (with-mongo-do test-db-uri
        (fn [mongo-db]
          (let [store (mongo/create-database mongo-db)
                item {:some-index-key "barry" :some-other-key "other"}]
            (c/find-maps mongo-db collection-name) => empty?
            (mongo/store! store collection-name :some-index-key item) => item
            (count (c/find-maps mongo-db collection-name)) => 1
            (c/find-one-as-map mongo-db collection-name {:some-index-key "barry"}) => (contains item)))))

(fact "storing an item with a duplicate key throws an exception"
      (with-mongo-do test-db-uri
        (fn [mongo-db]
          (let [store (mongo/create-database mongo-db)
                item {:some-index-key "barry" :some-other-key "other"}]
            (mongo/store! store collection-name :some-index-key item) => item
            (mongo/store! store collection-name :some-index-key item) => (throws Exception)))))

(fact "find-item queries items based on a query map and returns one if a match is found"
      (with-mongo-do test-db-uri
        (fn [mongo-db]
          (let [store (mongo/create-database mongo-db)
                item1 {:some-index-key "barry" :some-other-key "other"}
                item2 {:some-index-key "rebecca" :some-other-key "bsaa"}
                item3 {:some-index-key "zane"    :some-other-key "foo" :a-third-key "bar"}
                _ (mongo/store! store collection-name :some-index-key item1)
                _ (mongo/store! store collection-name :some-index-key item2)
                _ (mongo/store! store collection-name :some-index-key item3)]
            (mongo/find-item store collection-name {:some-other-key "other"}) => item1
            (mongo/find-item store collection-name {:some-other-key "bsaa"}) => item2
            (mongo/find-item store collection-name {:some-other-key "foo" :a-third-key "bar"}) => item3
            (mongo/find-item store collection-name {:some-other-key "nonExisty"}) => nil
            (mongo/find-item store collection-name nil) => nil))))

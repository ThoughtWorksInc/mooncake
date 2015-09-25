(ns mooncake.integration.db.mongo
  (:require
    [midje.sweet :refer :all]
    [monger.collection :as c]
    [mooncake.db.mongo :as mongo]
    [mooncake.test.test-helpers.db :as dbh]))

(def collection-name "stuff")

(defn test-fetch [database db-type]
  (fact {:midje/name (str db-type " -- creating mongo store from mongo uri creates a MongoDatabase which can be used to store-with-id! and fetch")}
        (mongo/store-with-id! database collection-name :some-index-key {:some-index-key "id" :some-other-key "other"})
        (mongo/fetch database collection-name "id") => {:some-index-key "id" :some-other-key "other"}
        (fact {:midje/name (str db-type " -- fetching an item that was stored with stringified keys returns one with keywordised keys")}
              (mongo/store-with-id! database collection-name "some-other-index-key" {"some-other-index-key" "another-id" "some-other-key" "other"})
              (mongo/fetch database collection-name "id") => {:some-index-key "id" :some-other-key "other"})))

(defn test-fetch-all [database db-type]
  (fact {:midje/name (str db-type " -- can fetch all items in a collection with keywordised keys")}
        (let [an-item {:a-key "barry"}
              another-item {:a-key "rebecca"}]
          (mongo/store! database collection-name an-item)
          (mongo/store! database collection-name another-item)
          (mongo/fetch-all database collection-name) => (just [{:a-key "barry"} {:a-key "rebecca"}] :in-any-order))))

(defn test-store-with-id [database db-type]
  (fact {:midje/name (str db-type " -- storing an item in an empty collection results in just that item being in the collection")}
        (let [item {:some-index-key "barry" :some-other-key "other"}]
          (mongo/fetch-all database collection-name) => empty?
          (mongo/store-with-id! database collection-name :some-index-key item) => item
          (count (mongo/fetch-all database collection-name)) => 1
          (mongo/find-item database collection-name {:some-index-key "barry"}) => item)))

(defn test-duplicate-key [database db-type]
  (fact {:midje/name (str db-type " -- storing an item with a duplicate key throws an exception")}
        (let [item {:some-index-key "barry" :some-other-key "other"}]
          (mongo/store-with-id! database collection-name :some-index-key item) => item
          (mongo/store-with-id! database collection-name :some-index-key item) => (throws Exception))))

(defn test-find-item [database db-type]
  (fact {:midje/name (str db-type " -- find-item queries items based on a query map and returns one if a match is found")}
        (let [item1 {:some-index-key "barry" :some-other-key "other"}
              item2 {:some-index-key "rebecca" :some-other-key "bsaa"}
              item3 {:some-index-key "zane" :some-other-key "foo" :a-third-key "bar"}
              _ (mongo/store-with-id! database collection-name :some-index-key item1)
              _ (mongo/store-with-id! database collection-name :some-index-key item2)
              _ (mongo/store-with-id! database collection-name :some-index-key item3)]
          (mongo/find-item database collection-name {:some-other-key "other"}) => item1
          (mongo/find-item database collection-name {:some-other-key "bsaa"}) => item2
          (mongo/find-item database collection-name {:some-other-key "foo" :a-third-key "bar"}) => item3
          (fact {:midje/name "check that non-existant item returns nil"}
                (mongo/find-item database collection-name {:some-other-key "nonExisty"}) => nil)
          (fact {:midje/name "returns nil if query is nil"}
                (mongo/find-item database collection-name nil) => nil)
          (fact {:midje/name "check that stringified key returns an item with keywordised keys"}
                (mongo/find-item database collection-name {"some-other-key" "other"}) => item1))))

(defn test-find-items-by-key-values [database db-type]
  (fact {:midje/name (str db-type " -- find-items-by-key-values queries items based on values of a single key and returns all matching items")}
        (let [item1 {:some-index-key "barry" :some-other-key "other"}
              item2 {:some-index-key "rebecca" :some-other-key "bsaa"}
              item3 {:some-index-key "zane" :some-other-key "foo" :a-third-key "bar"}
              item4 {:some-index-key "bob" :a-third-key "bar"}
              _ (mongo/store-with-id! database collection-name :some-index-key item1)
              _ (mongo/store-with-id! database collection-name :some-index-key item2)
              _ (mongo/store-with-id! database collection-name :some-index-key item3)
              _ (mongo/store-with-id! database collection-name :some-index-key item4)]
          (mongo/find-items-by-key-values database collection-name :some-other-key ["other"]) => [item1]
          (mongo/find-items-by-key-values database collection-name :some-index-key ["rebecca"]) => [item2]
          (mongo/find-items-by-key-values database collection-name :some-other-key ["other" "foo"]) => [item1 item3]
          (mongo/find-items-by-key-values database collection-name :a-third-key ["bar"]) => (just [item3 item4] :in-any-order)

          (fact {:midje/name "check that non-existant item returns an empty vector"}
                 (mongo/find-items-by-key-values database collection-name :some-other-key ["nonExisty"]) => [])
          (fact {:midje/name "check that non-existant key returns an empty vector"}
                (mongo/find-items-by-key-values database collection-name :non-existing-key ["nonExisty"]) => [])
          (fact {:midje/name "check that stringified key returns an item with keywordised keys"}
                (mongo/find-items-by-key-values database collection-name "some-other-key" ["other"]) => [item1]))))

(defn test-find-items-by-alternatives [database db-type]
  (fact {:midje/name (str db-type " -- test-find-items-by-alternatives queries items based on values of provided maps")}
        (let [item1 {:some-index-key "barry" :some-other-key "other"}
              item2 {:some-index-key "rebecca" :some-other-key "bsaa"}
              item3 {:some-index-key "zane" :some-other-key "foo" :a-third-key "bar"}
              _ (mongo/store-with-id! database collection-name :some-index-key item1)
              _ (mongo/store-with-id! database collection-name :some-index-key item2)
              _ (mongo/store-with-id! database collection-name :some-index-key item3)]
          (mongo/find-items-by-alternatives database collection-name [{:some-other-key "other"}]) => [item1]
          (mongo/find-items-by-alternatives database collection-name [{:some-other-key "other" :some-index-key "rebecca"}]) => []
          (mongo/find-items-by-alternatives database collection-name [{:some-index-key ["rebecca"]}]) => [item2]
          (mongo/find-items-by-alternatives database collection-name [{:some-other-key ["other" "foo"]}]) => (just [item1 item3] :in-any-order)

          (fact {:midje/name "check that non-existant item returns an empty vector"}
                (mongo/find-items-by-alternatives database collection-name [{:some-other-key ["nonExisty"]}]) => [])
          (fact {:midje/name "check that non-existant key returns an empty vector"}
                (mongo/find-items-by-alternatives database collection-name [{:non-existing-key ["nonExisty"]}]) => [])
          (fact {:midje/name "check that stringified key returns an item with keywordised keys"}
                (mongo/find-items-by-key-values database collection-name "some-other-key" ["other"]) => [item1])
          (fact "can query by multiple alternatives"
                (mongo/find-items-by-alternatives database collection-name [{:some-other-key ["other"]} {:some-index-key ["rebecca"]}]) => (just [item1 item2] :in-any-order)))))

(defn test-upsert [database db-type]
  (fact {:midje/name (str db-type " -- " (format "Implementation: [%s]: upsert inserts a record if it doesn't exist, or replaces it if found with query. " (type database)))}
        (mongo/upsert! database collection-name {:name "Gandalf"} {:name "Gandalf" :colour "white"})
        (mongo/fetch-all database collection-name) => [{:name "Gandalf" :colour "white"}]
        (mongo/upsert! database collection-name {:name "Gandalf"} {:name "Gandalf" :colour "grey"})
        (mongo/fetch-all database collection-name) => [{:name "Gandalf" :colour "grey"}]))

(def tests [test-fetch
            test-store-with-id
            test-upsert
            test-find-item
            test-find-items-by-key-values
            test-find-items-by-alternatives
            test-duplicate-key
            test-fetch-all])

(fact "test both implementations of database"
      (doseq [test tests]
        (dbh/with-mongo-do
          (fn [mongo-db]
            (let [mongo-version (mongo/create-database mongo-db)
                  in-memory-version (dbh/create-in-memory-db)]
              (test mongo-version "mongo")
              (test in-memory-version "in-memory"))))))
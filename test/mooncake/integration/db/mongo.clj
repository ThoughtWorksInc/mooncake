(ns mooncake.integration.db.mongo
  (:require
    [midje.sweet :refer :all]
    [monger.collection :as c]
    [mooncake.db.mongo :as mongo]
    [mooncake.test.test-helpers.db :as dbh]))

(def collection-name "stuff")

(defn test-fetch [database db-type]
  (fact {:midje/name "creating mongo store from mongo uri creates a MongoDatabase which can be used to store-with-id! and fetch"}
        (mongo/store-with-id! database collection-name :some-index-key {:some-index-key "barry" :some-other-key "other"})
        (mongo/fetch database collection-name "barry" true) => {:some-index-key "barry" :some-other-key "other"}
        (mongo/fetch database collection-name "barry" false) => {"some-index-key" "barry" "some-other-key" "other"}))

(defn test-store-with-id [database db-type]
  (fact {:midje/name "storing an item in an empty collection results in just that item being in the collection"}
        (let [item {:some-index-key "barry" :some-other-key "other"}]
          (mongo/fetch-all database collection-name true) => empty?
          (mongo/store-with-id! database collection-name :some-index-key item) => item
          (count (mongo/fetch-all database collection-name true)) => 1
          (mongo/find-item database collection-name {:some-index-key "barry"} true) => item)))

(defn test-duplicate-key [database db-type]
  (fact {:midje/name "storing an item with a duplicate key throws an exception"}
        (let [item {:some-index-key "barry" :some-other-key "other"}]
          (mongo/store-with-id! database collection-name :some-index-key item) => item
          (mongo/store-with-id! database collection-name :some-index-key item) => (throws Exception))))

(defn test-find-item [database db-type]
  (fact {:midje/name "find-item queries items based on a query map and returns one if a match is found"}
        (let [item1 {:some-index-key "barry" :some-other-key "other"}
              item2 {:some-index-key "rebecca" :some-other-key "bsaa"}
              item3 {:some-index-key "zane" :some-other-key "foo" :a-third-key "bar"}
              _ (mongo/store-with-id! database collection-name :some-index-key item1)
              _ (mongo/store-with-id! database collection-name :some-index-key item2)
              _ (mongo/store-with-id! database collection-name :some-index-key item3)]
          (mongo/find-item database collection-name {:some-other-key "other"} true) => item1
          (mongo/find-item database collection-name {:some-other-key "bsaa"} true) => item2
          (mongo/find-item database collection-name {:some-other-key "foo" :a-third-key "bar"} true) => item3
          (fact {:midje/name "check that non-existant item returns nil"}
                (mongo/find-item database collection-name {:some-other-key "nonExisty"} true) => nil)
          (fact {:midje/name "returns nil if query is nil"}
                (mongo/find-item database collection-name nil true) => nil)
          (fact "can turn off keywordisation of keys"
                (mongo/find-item database collection-name {:some-other-key "other"} false) => {"some-index-key" "barry" "some-other-key" "other"}))))

(defn test-find-items-by-key-values [database db-type]
  (fact {:midje/name (str "find-items-by-key-values queries items based on values of a single key and returns all matching items" " -- " db-type)}
        (let [item1 {:some-index-key "barry" :some-other-key "other"}
              item2 {:some-index-key "rebecca" :some-other-key "bsaa"}
              item3 {:some-index-key "zane" :some-other-key "foo" :a-third-key "bar"}
              item4 {:some-index-key "bob" :a-third-key "bar"}
              _ (mongo/store-with-id! database collection-name :some-index-key item1)
              _ (mongo/store-with-id! database collection-name :some-index-key item2)
              _ (mongo/store-with-id! database collection-name :some-index-key item3)
              _ (mongo/store-with-id! database collection-name :some-index-key item4)]
          (mongo/find-items-by-key-values database collection-name :some-other-key ["other"] true) => [item1]
          (mongo/find-items-by-key-values database collection-name :some-index-key ["rebecca"] true) => [item2]
          (mongo/find-items-by-key-values database collection-name :some-other-key ["other" "foo"] true) => [item1 item3]
          (mongo/find-items-by-key-values database collection-name :a-third-key ["bar"] true) => (just [item3 item4] :in-any-order)

          (fact {:midje/name "check that non-existant item returns an empty vector"}
                 (mongo/find-items-by-key-values database collection-name :some-other-key ["nonExisty"] true) => [])
          (fact {:midje/name "check that non-existant key returns an empty vector"}
                (mongo/find-items-by-key-values database collection-name :non-existing-key ["nonExisty"] true) => [])
          (fact "can turn off keywordisation of keys"
                (mongo/find-items-by-key-values database collection-name "some-other-key" ["other"] false) => [{"some-index-key" "barry" "some-other-key" "other"}]))))

(defn test-find-items-by-alternatives [database db-type]
  (fact {:midje/name (str "test-find-items-by-alternatives queries items based on values of provided maps -- " db-type)}
        (let [item1 {:some-index-key "rebecca" :some-other-key "other"}
              item2 {:some-index-key "barry" :some-other-key "bsaa"}
              item3 {:some-index-key "zane" :some-other-key "foo" :a-third-key "bar"}
              _ (mongo/store-with-id! database collection-name :some-index-key item1)
              _ (mongo/store-with-id! database collection-name :some-index-key item2)
              _ (mongo/store-with-id! database collection-name :some-index-key item3)]
          (mongo/find-items-by-alternatives database collection-name [{:some-other-key "other"}] {:stringify? false}) => [item1]
          (mongo/find-items-by-alternatives database collection-name [{:some-other-key "other" :some-index-key "barry"}] {:stringify? false}) => []
          (mongo/find-items-by-alternatives database collection-name [{:some-index-key ["barry"]}] {:stringify? false}) => [item2]
          (mongo/find-items-by-alternatives database collection-name [{:some-other-key ["other" "foo"]}] {:stringify? false}) => (just [item1 item3] :in-any-order)

          (fact {:midje/name "check that non-existant item returns an empty vector"}
                (mongo/find-items-by-alternatives database collection-name [{:some-other-key ["nonExisty"]}] {:stringify? false}) => [])
          (fact {:midje/name "check that non-existant key returns an empty vector"}
                (mongo/find-items-by-alternatives database collection-name [{:non-existing-key ["nonExisty"]}] {:stringify? false}) => [])
          (fact "can turn off keywordisation of keys"
                (mongo/find-items-by-alternatives database collection-name [{"some-other-key" ["other"]}] {:stringify? true}) => [{"some-index-key" "rebecca" "some-other-key" "other"}])
          (fact "can query by multiple alternatives"
                (mongo/find-items-by-alternatives database collection-name [{:some-other-key ["other"]} {:some-index-key ["barry"]}] {:stringify? false}) => (just [item1 item2] :in-any-order))
          (fact "can sort results by a given column and ordering"
                (mongo/find-items-by-alternatives database collection-name [{}] {:stringify? false :sort {:some-index-key :ascending}}) => (just [item2 item1 item3])
                (mongo/find-items-by-alternatives database collection-name [{}] {:stringify? false :sort {:some-index-key :descending}}) => (just [item3 item1 item2]))
          (fact "only supports sorting by one key"
                (mongo/find-items-by-alternatives database collection-name [{}] {:stringify? false :sort {:some-index-key :ascending :other-index-key :descending}}) => (throws anything)))))

(defn test-fetch-all-items-with-stringified-keys [database db-type]
  (fact {:midje/name "can fetch all items in a collection with stringified keys"}
        (let [item1 {:a-key1 1}
              item2 {:a-key2 2}]
          (mongo/store! database collection-name item1)
          (mongo/store! database collection-name item2)
          (mongo/fetch-all database collection-name false) => (just [{"a-key1" 1} {"a-key2" 2}] :in-any-order))))

(defn test-fetch-all-items-with-keywordised-keys [database db-type]
  (fact {:midje/name "can fetch all items in a collection with keywordised keys"}
        (let [keywordised-item1 {:keywordised1 1}
              keywordised-item2 {:keywordised2 2}]
          (mongo/store! database collection-name keywordised-item1)
          (mongo/store! database collection-name keywordised-item2)
          (mongo/fetch-all database collection-name true) => (just [{:keywordised1 1} {:keywordised2 2}] :in-any-order))))

(defn test-upsert [database db-type]
  (fact {:midje/name (format "Implementation: [%s]: upsert inserts a record if it doesn't exist, or replaces it if found with query. " (type database))}
        (mongo/upsert! database collection-name {:name "Gandalf"} {:name "Gandalf" :colour "white"})
        (mongo/fetch-all database collection-name true) => [{:name "Gandalf" :colour "white"}]
        (mongo/upsert! database collection-name {:name "Gandalf"} {:name "Gandalf" :colour "grey"})
        (mongo/fetch-all database collection-name true) => [{:name "Gandalf" :colour "grey"}]))

(def tests [test-fetch
            test-store-with-id
            test-upsert
            test-find-item
            test-find-items-by-key-values
            test-find-items-by-alternatives
            test-duplicate-key
            test-fetch-all-items-with-stringified-keys
            test-fetch-all-items-with-keywordised-keys])

(fact "test both implementations of database"
      (doseq [test tests]
        (dbh/with-mongo-do
          (fn [mongo-db]
            (let [mongo-version (mongo/create-database mongo-db)
                  in-memory-version (dbh/create-in-memory-db)]
              (test mongo-version "mongo")
              (test in-memory-version "in-memory"))))))
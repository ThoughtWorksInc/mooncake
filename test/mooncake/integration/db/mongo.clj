(ns mooncake.integration.db.mongo
  (:require
    [midje.sweet :refer :all]
    [mooncake.db.mongo :as mongo]
    [mooncake.test.test-helpers.db :as dbh]))

(def collection-name "stuff")

(defn test-fetch [store]
  (fact {:midje/name (str (type store) " -- creating mongo store from mongo uri creates a MongoStore which can be used to store-with-id! and fetch")}
        (mongo/store-with-id! store collection-name :some-index-key {:some-index-key "barry" :some-other-key "other"})
        (mongo/fetch store collection-name "barry" {:stringify? false}) => {:some-index-key "barry" :some-other-key "other"}
        (mongo/fetch store collection-name "barry" {:stringify? true}) => {"some-index-key" "barry" "some-other-key" "other"}))

(defn test-store-with-id [store]
  (fact {:midje/name (str (type store) " -- storing an item in an empty collection results in just that item being in the collection")}
        (let [item {:some-index-key "barry" :some-other-key "other"}]
          (mongo/fetch-all store collection-name {:stringify? false}) => empty?
          (mongo/store-with-id! store collection-name :some-index-key item) => item
          (count (mongo/fetch-all store collection-name {:stringify? false})) => 1
          (mongo/find-item store collection-name {:some-index-key "barry"} {:stringify? false}) => item)))

(defn test-duplicate-key [store]
  (fact {:midje/name (str (type store) " -- storing an item with a duplicate key throws an exception")}
        (let [item {:some-index-key "barry" :some-other-key "other"}]
          (mongo/store-with-id! store collection-name :some-index-key item) => item
          (mongo/store-with-id! store collection-name :some-index-key item) => (throws Exception))))

(defn test-find-item [store]
  (fact {:midje/name (str (type store) " -- find-item queries items based on a query map and returns one if a match is found")}
        (let [item1 {:some-index-key "barry" :some-other-key "other"}
              item2 {:some-index-key "rebecca" :some-other-key "bsaa"}
              item3 {:some-index-key "zane" :some-other-key "foo" :a-third-key "bar"}
              _ (mongo/store-with-id! store collection-name :some-index-key item1)
              _ (mongo/store-with-id! store collection-name :some-index-key item2)
              _ (mongo/store-with-id! store collection-name :some-index-key item3)]
          (mongo/find-item store collection-name {:some-other-key "other"} {:stringify? false}) => item1
          (mongo/find-item store collection-name {:some-other-key "bsaa"} {:stringify? false}) => item2
          (mongo/find-item store collection-name {:some-other-key "foo" :a-third-key "bar"} {:stringify? false}) => item3
          (fact {:midje/name "check that non-existant item returns nil"}
                (mongo/find-item store collection-name {:some-other-key "nonExisty"} {:stringify? false}) => nil)
          (fact {:midje/name "returns nil if query is nil"}
                (mongo/find-item store collection-name nil {:stringify? false}) => nil)
          (fact "can turn off keywordisation of keys"
                (mongo/find-item store collection-name {:some-other-key "other"} {:stringify? true}) => {"some-index-key" "barry" "some-other-key" "other"}))))

(defn test-find-items-by-alternatives [store]
  (fact {:midje/name (str (type store) " -- test-find-items-by-alternatives queries items based on values of provided maps")}
        (let [item1 {:some-index-key "rebecca" :some-other-key "other"}
              item2 {:some-index-key "barry" :some-other-key "bsaa"}
              item3 {:some-index-key "zane" :some-other-key "foo" :a-third-key "bar"}
              _ (mongo/store-with-id! store collection-name :some-index-key item1)
              _ (mongo/store-with-id! store collection-name :some-index-key item2)
              _ (mongo/store-with-id! store collection-name :some-index-key item3)]
          (mongo/find-items-by-alternatives store collection-name [{:some-other-key "other"}] {:stringify? false}) => [item1]
          (mongo/find-items-by-alternatives store collection-name [{:some-other-key "other" :some-index-key "barry"}] {:stringify? false}) => []
          (mongo/find-items-by-alternatives store collection-name [{:some-index-key ["barry"]}] {:stringify? false}) => [item2]
          (mongo/find-items-by-alternatives store collection-name [{:some-other-key ["other" "foo"]}] {:stringify? false}) => (just [item1 item3] :in-any-order)
          (fact {:midje/name "check that non-existant item returns an empty vector"}
                (mongo/find-items-by-alternatives store collection-name [{:some-other-key ["nonExisty"]}] {:stringify? false}) => [])
          (fact {:midje/name "check that non-existant key returns an empty vector"}
                (mongo/find-items-by-alternatives store collection-name [{:non-existing-key ["nonExisty"]}] {:stringify? false}) => [])
          (fact "can turn off keywordisation of keys"
                (mongo/find-items-by-alternatives store collection-name [{"some-other-key" ["other"]}] {:stringify? true}) => [{"some-index-key" "rebecca" "some-other-key" "other"}])
          (fact "can query by multiple alternatives"
                (mongo/find-items-by-alternatives store collection-name [{:some-other-key ["other"]} {:some-index-key ["barry"]}] {:stringify? false}) => (just [item1 item2] :in-any-order))
          (fact "can sort results by a given column and ordering"
                (mongo/find-items-by-alternatives store collection-name [{}] {:stringify? false :sort {:some-index-key :ascending}}) => (just [item2 item1 item3])
                (mongo/find-items-by-alternatives store collection-name [{}] {:stringify? false :sort {:some-index-key :descending}}) => (just [item3 item1 item2]))
          (fact "only supports sorting by one key"
                (mongo/find-items-by-alternatives store collection-name [{}] {:stringify? false :sort {:some-index-key :ascending :other-index-key :descending}}) => (throws anything))
          (fact "can retrieve results in batches"
                (mongo/find-items-by-alternatives store collection-name [{}] {:stringify? false :limit 2}) => (two-of anything)))))

(defn test-fetch-all-items-with-stringified-keys [store]
  (fact {:midje/name (str (type store) " -- can fetch all items in a collection with stringified keys")}
        (let [item1 {:a-key1 1}
              item2 {:a-key2 2}]
          (mongo/store! store collection-name item1)
          (mongo/store! store collection-name item2)
          (mongo/fetch-all store collection-name {:stringify? true}) => (just [{"a-key1" 1} {"a-key2" 2}] :in-any-order))))

(defn test-fetch-all-items-with-keywordised-keys [store]
  (fact {:midje/name (str (type store) " -- can fetch all items in a collection with keywordised keys")}
        (let [keywordised-item1 {:keywordised1 1}
              keywordised-item2 {:keywordised2 2}]
          (mongo/store! store collection-name keywordised-item1)
          (mongo/store! store collection-name keywordised-item2)
          (mongo/fetch-all store collection-name {:stringify? false}) => (just [{:keywordised1 1} {:keywordised2 2}] :in-any-order))))

(defn test-upsert [store]
  (fact {:midje/name (str (type store) " -- upsert inserts a record if it doesn't exist, or replaces it if found with query")}
        (mongo/upsert! store collection-name {:name "Gandalf"} :colour "white")
        (mongo/fetch-all store collection-name {:stringify? false}) => [{:name "Gandalf" :colour "white"}]
        (mongo/upsert! store collection-name {:name "Gandalf"} :colour "grey")
        (mongo/fetch-all store collection-name {:stringify? false}) => [{:name "Gandalf" :colour "grey"}]))

(defn bugfix-test-store-with-id-and-then-upsert [store]
  (fact {:midje/name (str (type store) " -- upsert works correctly after first storing with id")}
        (mongo/store-with-id! store collection-name :name {:name "Gandalf"})
        (mongo/fetch-all store collection-name {:stringify? false}) => [{:name "Gandalf"}]
        (mongo/upsert! store collection-name {:name "Gandalf"} :colour "grey")
        (mongo/fetch-all store collection-name {:stringify? false}) => [{:name "Gandalf" :colour "grey"}]))

(defn test-add-to-set [store]
  (fact {:midje/name (str (type store) " -- add-to-set adds a single value to an array field, ensuring there are no duplicates")}
        (fact "can add first value to a set"
              (mongo/add-to-set! store collection-name {:name "Gandalf"} :beard-colours "white")
              (mongo/fetch-all store collection-name {:stringify? false}) => [{:name "Gandalf" :beard-colours ["white"]}])
        (fact "can add second value to a set"
              (mongo/add-to-set! store collection-name {:name "Gandalf"} :beard-colours "grey")
              (mongo/fetch-all store collection-name {:stringify? false}) => [{:name "Gandalf" :beard-colours ["white" "grey"]}])
        (fact "does not add duplicates to set"
              (mongo/add-to-set! store collection-name {:name "Gandalf"} :beard-colours "grey")
              (mongo/fetch-all store collection-name {:stringify? false}) => [{:name "Gandalf" :beard-colours ["white" "grey"]}])))

(def tests [test-fetch
            test-store-with-id
            test-upsert
            test-find-item
            test-find-items-by-alternatives
            test-duplicate-key
            test-fetch-all-items-with-stringified-keys
            test-fetch-all-items-with-keywordised-keys
            bugfix-test-store-with-id-and-then-upsert
            test-add-to-set])

(fact "test both implementations of store"
      (doseq [test tests]
        (dbh/with-mongo-do
          (fn [mongo-db]
            (let [mongo-store (mongo/create-mongo-store mongo-db)
                  in-memory-store (dbh/create-in-memory-store)]
              (test mongo-store)
              (test in-memory-store))))))
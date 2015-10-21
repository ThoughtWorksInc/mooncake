(ns mooncake.integration.db.mongo
  (:require
    [midje.sweet :refer :all]
    [mooncake.db.mongo :as mongo]
    [mooncake.test.test-helpers.db :as dbh]))

(def collection-name "stuff")

(defn test-fetch [store]
  (fact {:midje/name (str (type store) " -- creating mongo store from mongo uri creates a MongoStore which can be used to store-with-id! and fetch")}
        (mongo/store-with-id! store collection-name :some-index-key {:some-index-key "barry" :some-other-key "other"})
        (mongo/fetch store collection-name "barry") => {:some-index-key "barry" :some-other-key "other"}))

(defn test-store-with-id [store]
  (fact {:midje/name (str (type store) " -- storing an item in an empty collection results in just that item being in the collection")}
        (let [item {:some-index-key "barry" :some-other-key "other"}]
          (mongo/fetch-all store collection-name) => empty?
          (mongo/store-with-id! store collection-name :some-index-key item) => item
          (count (mongo/fetch-all store collection-name)) => 1
          (mongo/find-item store collection-name {:some-index-key "barry"}) => item)))

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
          (mongo/find-item store collection-name {:some-other-key "other"}) => item1
          (mongo/find-item store collection-name {:some-other-key "bsaa"}) => item2
          (mongo/find-item store collection-name {:some-other-key "foo" :a-third-key "bar"}) => item3
          (fact {:midje/name "check that non-existant item returns nil"}
                (mongo/find-item store collection-name {:some-other-key "nonExisty"}) => nil)
          (fact {:midje/name "returns nil if query is nil"}
                (mongo/find-item store collection-name nil) => nil))))

(defn test-find-items-by-alternatives [store]
  (fact {:midje/name (str (type store) " -- test-find-items-by-alternatives queries items based on values of provided maps")}
        (let [item1 {:some-index-key "rebecca" :some-other-key "other"}
              item2 {:some-index-key "barry" :some-other-key "bsaa"}
              item3 {:some-index-key "zane" :some-other-key "foo" :a-third-key "bar"}
              _ (mongo/store-with-id! store collection-name :some-index-key item1)
              _ (mongo/store-with-id! store collection-name :some-index-key item2)
              _ (mongo/store-with-id! store collection-name :some-index-key item3)]
          (mongo/find-items-by-alternatives store collection-name [{:some-other-key "other"}] {}) => [item1]
          (mongo/find-items-by-alternatives store collection-name [{:some-other-key "other" :some-index-key "barry"}] {}) => []
          (mongo/find-items-by-alternatives store collection-name [{:some-index-key ["barry"]}] {}) => [item2]
          (mongo/find-items-by-alternatives store collection-name [{:some-other-key ["other" "foo"]}] {}) => (just [item1 item3] :in-any-order)
          (fact {:midje/name "check that non-existant item returns an empty vector"}
                (mongo/find-items-by-alternatives store collection-name [{:some-other-key ["nonExisty"]}] {}) => [])
          (fact {:midje/name "check that non-existant key returns an empty vector"}
                (mongo/find-items-by-alternatives store collection-name [{:non-existing-key ["nonExisty"]}] {}) => [])
          (fact "can query by multiple alternatives"
                (mongo/find-items-by-alternatives store collection-name [{:some-other-key ["other"]} {:some-index-key ["barry"]}] {}) => (just [item1 item2] :in-any-order))
          (fact "can sort results by a given column and ordering"
                (mongo/find-items-by-alternatives store collection-name [{}] {:sort {:some-index-key :ascending}}) => (just [item2 item1 item3])
                (mongo/find-items-by-alternatives store collection-name [{}] {:sort {:some-index-key :descending}}) => (just [item3 item1 item2]))
          (fact "only supports sorting by one key"
                (mongo/find-items-by-alternatives store collection-name [{}] {:sort {:some-index-key :ascending :other-index-key :descending}}) => (throws anything))
          (fact "can retrieve results in batches"
                (mongo/find-items-by-alternatives store collection-name [{}] {:limit 2}) => (two-of anything))
          (fact "can retrieve results by page"
                (mongo/find-items-by-alternatives store collection-name [{}] {:limit 2 :page-number 2}) => (one-of anything))
          (fact "if page number is nil it will default to the first page"
                (mongo/find-items-by-alternatives store collection-name [{}] {:limit 2 :page-number nil}) => (two-of anything)))))

(defn test-find-items-by-timestamps [store]
  (fact {:midje/name (str (type store) " -- test-find-items-by-timestamp queries items that are older than provided timestamp")}
        (let [latest-time "2015-08-12T00:00:02.000Z"
              second-latest-time "2015-08-12T00:00:01.000Z"
              oldest-time "2015-08-12T00:00:00.000Z"
              item1 {:some-index-key "rebecca" :published latest-time}
              item2 {:some-index-key "barry" :published second-latest-time}
              item3 {:some-index-key "zane" :published oldest-time}
              _ (mongo/store-with-id! store collection-name :some-index-key item1)
              _ (mongo/store-with-id! store collection-name :some-index-key item2)
              _ (mongo/store-with-id! store collection-name :some-index-key item3)]

          (mongo/find-items-by-timestamp store collection-name [{}] {} second-latest-time) => (just [item3]))))

(defn test-fetch-total-count-by-query [store]
  (fact {:midje/name (str (type store) " -- test-fetch-total-count-by-query gets the total number of items which match the query")}
        (let [item1 {:some-index-key "rebecca" :some-other-key "other"}
              item2 {:some-index-key "barry" :some-other-key "other"}
              item3 {:some-index-key "zane" :some-other-key "foo" :a-third-key "bar"}
              _ (mongo/store-with-id! store collection-name :some-index-key item1)
              _ (mongo/store-with-id! store collection-name :some-index-key item2)
              _ (mongo/store-with-id! store collection-name :some-index-key item3)]
          (mongo/fetch-total-count-by-query store collection-name [{}]) => 3
          (mongo/fetch-total-count-by-query store collection-name [{:some-other-key "other"}]) => 2)))

(defn test-fetch-all-items [store]
  (fact {:midje/name (str (type store) " -- can fetch all items")}
        (let [item1 {:a-key 1}
              item2 {:another-key 2}]
          (mongo/store! store collection-name item1)
          (mongo/store! store collection-name item2)
          (mongo/fetch-all store collection-name) => (just [{:a-key 1} {:another-key 2}] :in-any-order))))

(defn test-upsert [store]
  (fact {:midje/name (str (type store) " -- upsert inserts a record if it doesn't exist, or replaces it if found with query")}
        (mongo/upsert! store collection-name {:name "Gandalf"} :colour "white")
        (mongo/fetch-all store collection-name) => [{:name "Gandalf" :colour "white"}]
        (mongo/upsert! store collection-name {:name "Gandalf"} :colour "grey")
        (mongo/fetch-all store collection-name) => [{:name "Gandalf" :colour "grey"}]))

(defn bugfix-test-store-with-id-and-then-upsert [store]
  (fact {:midje/name (str (type store) " -- upsert works correctly after first storing with id")}
        (mongo/store-with-id! store collection-name :name {:name "Gandalf"})
        (mongo/fetch-all store collection-name) => [{:name "Gandalf"}]
        (mongo/upsert! store collection-name {:name "Gandalf"} :colour "grey")
        (mongo/fetch-all store collection-name) => [{:name "Gandalf" :colour "grey"}]))

(defn test-add-to-set [store]
  (fact {:midje/name (str (type store) " -- add-to-set adds a single value to an array field, ensuring there are no duplicates")}
        (fact "can add first value to a set"
              (mongo/add-to-set! store collection-name {:name "Gandalf"} :beard-colours "white")
              (mongo/fetch-all store collection-name) => [{:name "Gandalf" :beard-colours ["white"]}])
        (fact "can add second value to a set"
              (mongo/add-to-set! store collection-name {:name "Gandalf"} :beard-colours "grey")
              (mongo/fetch-all store collection-name) => [{:name "Gandalf" :beard-colours ["white" "grey"]}])
        (fact "does not add duplicates to set"
              (mongo/add-to-set! store collection-name {:name "Gandalf"} :beard-colours "grey")
              (mongo/fetch-all store collection-name) => [{:name "Gandalf" :beard-colours ["white" "grey"]}])))

(def tests [test-fetch
            test-store-with-id
            test-upsert
            test-find-item
            test-find-items-by-alternatives
            test-find-items-by-timestamps
            test-fetch-total-count-by-query
            test-duplicate-key
            test-fetch-all-items
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
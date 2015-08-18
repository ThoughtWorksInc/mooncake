(ns mooncake.integration.db.mongo
  (:require
    [midje.sweet :refer :all]
    [monger.core :as m]
    [monger.collection :as c]
    [mooncake.db.mongo :as mongo]))

(def test-db "mooncake-test")
(def collection-name "stuff")

(def db (atom nil))
(def conn (atom nil))

(defn connect []
  (swap! conn (constantly (m/connect)))
  (swap! db (constantly (m/get-db @conn test-db))))

(defn clear-data []
  (m/drop-db @conn test-db))

(defn disconnect []
  (m/disconnect @conn))

(background
  (before :facts (do (connect) (clear-data))
          :after (do (clear-data) (disconnect))))

(fact "creating mongo store from mongo uri creates a MongoDatabase which can be used to store! and fetch"
      (let [store (-> (mongo/get-mongo-db (str "mongodb://localhost:27017/" test-db))
                      (mongo/create-mongo-store collection-name))]
        (class store) => mooncake.db.mongo.MongoDatabase
        (mongo/store! store :some-index-key {:some-index-key "barry" :some-other-key "other"})
        (mongo/fetch store "barry") => {:some-index-key "barry" :some-other-key "other"}))

(fact "storing an item in an empty collection results in just that item being in the collection"
      (let [mongo-db (mongo/get-mongo-db (str "mongodb://localhost:27017/" test-db))
            store (mongo/create-mongo-store mongo-db collection-name)
            item {:some-index-key "barry" :some-other-key "other"}]
        (c/find-maps mongo-db collection-name) => empty?
        (mongo/store! store :some-index-key item) => item
        (count (c/find-maps mongo-db collection-name)) => 1
        (c/find-one-as-map mongo-db collection-name {:some-index-key "barry"}) => (contains item)))

(fact "storing an item with a duplicate key throws an exception"
      (let [mongo-db (mongo/get-mongo-db (str "mongodb://localhost:27017/" test-db))
            store (mongo/create-mongo-store mongo-db collection-name)
            item {:some-index-key "barry" :some-other-key "other"}]
        (mongo/store! store :some-index-key item) => item
        (mongo/store! store :some-index-key item) => (throws Exception)))

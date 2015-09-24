(ns mooncake.test.db.mongo
  (:require
    [midje.sweet :refer :all]
    [monger.collection :as c]
    [monger.operators :as mop]
    [mooncake.db.mongo :as mongo]
    [mooncake.test.test-helpers.db :as dbh]))

(facts "about value-map->mongo-query-map"
       (fact "creates query map with the same values if provided map has no nested collections"
             (mongo/value-map->mongo-query-map {:a "1" :b "2" :c "3"}) => {:a "1" :b "2" :c "3"})
       (fact "creates query map with mongo $in statements if provided map has nested collections"
             (mongo/value-map->mongo-query-map {:a "1" :b ["2" "3"] :c ["4" "5"]})
              => {:a "1" :b {mop/$in ["2" "3"]} :c {mop/$in ["4" "5"]}})
       (fact "creates empty query map if provided map is empty"
             (mongo/value-map->mongo-query-map {}) => {}))

(facts "about value-map-list->mongo-or-query-map"
       (fact "joins elements with mongo $or keyword"
             (mongo/value-map-vector->or-mongo-query-map [{:a "1"} {:b "2"}]) => {mop/$or [{:a "1"} {:b "2"}]})
       (fact "converts elements to mongo queries"
             (mongo/value-map-vector->or-mongo-query-map [{:a ["1", "2"]} {:b "2"}]) => {mop/$or [{:a {mop/$in ["1" "2"]}} {:b "2"}]}))





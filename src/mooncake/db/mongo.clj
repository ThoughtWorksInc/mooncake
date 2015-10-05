(ns mooncake.db.mongo
  (:require [monger.core :as mcore]
            [monger.operators :as mop]
            [monger.collection :as mcoll]
            [clojure.tools.logging :as log]
            [clojure.walk :as walk]
            [mooncake.helper :as mh]))

(defprotocol Store
  (fetch [this coll k options-m]
    "Find the item based on a key.")
  (fetch-all [this coll options-m]
    "Find all items based on a collection.")
  (find-item [this coll query-m options-m]
    "Find an item matching the query-map.")
  (find-items-by-alternatives [this coll value-map-vector options-m]
    "Find items whose properties match properties of at least one of the provided maps.")
  (store! [this coll item]
    "Store the given map and return it.")
  (store-with-id! [this coll key-param item]
    "Store the given map using the value of the kw key-param and return it.")
  (upsert! [this coll query key-param value]
    "Update item that corresponds to query by replacing key-param with value, or if none exist insert it.")
  (add-to-set! [this coll query key-param value]
    "Add value to the key-param array in the item found with query, ensuring there are no duplicates."))

(defn dissoc-id
  ([item]
   (dissoc-id item true))
  ([item keywordise?]
   (dissoc item (if keywordise? :_id "_id"))))

(defn value->mongo-query-value [value]
  (if (sequential? value)
    {mop/$in value}
    value))

(defn value-map->mongo-query-map [value-m]
  (reduce-kv #(assoc %1 %2 (value->mongo-query-value %3)) {} value-m))

(defn value-map-vector->or-mongo-query-map [value-map-vector]
  {mop/$or (map value-map->mongo-query-map value-map-vector)})

(defn options-m->sort-query-map [options-m]
  (-> (:sort options-m)
      (mh/map-over-values {:ascending 1 :descending -1})))

(defn stringify [collection stringify?]
  (cond-> collection
          stringify? walk/stringify-keys))

(defrecord MongoStore [mongo-db]
  Store
  (fetch [this coll k options-m]
    (let [stringify? (:stringify? options-m)]
      (when k
        (-> (mcoll/find-map-by-id mongo-db coll k [] (not stringify?))
            (stringify stringify?)
            (dissoc-id (not stringify?))))))

  (fetch-all [this coll options-m]
    (let [result-m (->> (mcoll/find-maps mongo-db coll)
                        (map dissoc-id))]
      (stringify result-m (:stringify? options-m))))

  (find-item [this coll query-m options-m]
    (let [stringify? (:stringify? options-m)]
      (when query-m
        (-> (mcoll/find-one-as-map mongo-db coll query-m [] (not stringify?))
            (dissoc-id (not stringify?))))))

  (find-items-by-alternatives [this coll value-map-vector options-m]
    (when (< 1 (count (keys (:sort options-m)))) (throw (ex-info "Trying to sort by more than one key" (:sort options-m))))
    (if (not-empty value-map-vector)
      (let [mongo-query-map (value-map-vector->or-mongo-query-map value-map-vector)
            sort-query-map (options-m->sort-query-map options-m)
            batch-size (:limit options-m)
            stringify? (:stringify? options-m)
            aggregation-pipeline (cond-> []
                                         :always (conj {mop/$match mongo-query-map})
                                         (not (empty? sort-query-map)) (conj {mop/$sort sort-query-map})
                                         (not (nil? batch-size)) (conj {mop/$limit batch-size}))
            result-m (->> (mcoll/aggregate mongo-db coll aggregation-pipeline)
                          (map dissoc-id))]
        (stringify result-m stringify?))
      []))

  (store! [this coll item]
    (-> (mcoll/insert-and-return mongo-db coll item)
        (dissoc :_id)))

  (store-with-id! [this coll key-param item]
    (->> (assoc item :_id (key-param item))
         (store! this coll)))

  (upsert! [this coll query key-param value]
    (mcoll/update mongo-db coll query {mop/$set {key-param value}} {:upsert true}))

  (add-to-set! [this coll query key-param value]
    (mcoll/update mongo-db coll query {mop/$addToSet {key-param value}} {:upsert true})))



(defn create-mongo-store [mongodb]
  (MongoStore. mongodb))

(defn get-mongo-db-and-conn [mongo-uri]
  (log/debug "Connecting to mongo.")
  (let [db-and-conn (mcore/connect-via-uri mongo-uri)]
    (log/debug "Connected to mongo.")
    db-and-conn))

(defn get-mongo-db [mongo-uri]
  (:db (get-mongo-db-and-conn mongo-uri)))
(ns mooncake.db.mongo
  (:require [monger.core :as mcore]
            [monger.operators :as mop]
            [monger.collection :as mcoll]
            [clojure.tools.logging :as log]
            [clojure.walk :as walk]
            [mooncake.helper :as mh]))

(defprotocol Database
  (fetch [this coll k keywordise?]
    "Find the item based on a key.")
  (fetch-all [this coll keywordise?]
    "Find all items based on a collection.")
  (find-item [this coll query-m keywordise?]
    "Find an item matching the query-map.")
  (find-items-by-key-values [this coll k values keywordise?]
    "Find items whose key 'k' matches one of the given values.")
  (find-items-by-alternatives [this coll value-map-vector options-m]
    "Find items whose properties match properties of at least one of the provided maps.")
  (store! [this coll item]
    "Store the given map and return it.")
  (store-with-id! [this coll key-param item]
    "Store the given map using the value of the kw key-param and return it.")
  (upsert! [this coll query item]
    "Update item that corresponds to query, or if none exist insert item."))

(defn dissoc-id
  ([item]
   (dissoc-id item true))
  ([item keywordise?]
   (dissoc item (if keywordise? :_id "_id"))))

(defn key-values->mongo-query-map [k values]
  {k {mop/$in values}})

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

(defn keywordise [collection keywordise?]
  (if keywordise?
    collection
    (walk/stringify-keys collection)))

(defrecord MongoDatabase [mongo-db]
  Database
  (fetch [this coll k options-m]
    (let [stringify (:stringify? options-m)]
      (when k
        (-> (mcoll/find-map-by-id mongo-db coll k [] (not stringify))
            (keywordise (not stringify))
            (dissoc-id (not stringify))))))

  (fetch-all [this coll keywordise?]
    (let [result-m (->> (mcoll/find-maps mongo-db coll)
                        (map dissoc-id))]
      (keywordise result-m keywordise?)))

  (find-item [this coll query-m keywordise?]
    (when query-m
      (-> (mcoll/find-one-as-map mongo-db coll query-m [] keywordise?)
          (dissoc-id keywordise?))))

  (find-items-by-key-values [this coll k values keywordise?]
    (if values
      (let [mongo-key-in-query (key-values->mongo-query-map k values)
            result-m (->> (mcoll/find-maps mongo-db coll mongo-key-in-query)
                          (map dissoc-id))]
        (keywordise result-m keywordise?))
      []))

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
        (keywordise result-m (not stringify?)))
      []))

  (store! [this coll item]
    (-> (mcoll/insert-and-return mongo-db coll item)
        (dissoc :_id)))

  (store-with-id! [this coll key-param item]
    (->> (assoc item :_id (key-param item))
         (store! this coll)))

  (upsert! [this coll query item]
    (mcoll/upsert mongo-db coll query item)))

(defn create-database [mongodb]
  (MongoDatabase. mongodb))

(defn get-mongo-db-and-conn [mongo-uri]
  (log/debug "Connecting to mongo.")
  (let [db-and-conn (mcore/connect-via-uri mongo-uri)]
    (log/debug "Connected to mongo.")
    db-and-conn))

(defn get-mongo-db [mongo-uri]
  (:db (get-mongo-db-and-conn mongo-uri)))
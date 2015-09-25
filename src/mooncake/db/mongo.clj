(ns mooncake.db.mongo
  (:require [monger.core :as mcore]
            [monger.operators :as mop]
            [monger.collection :as mcoll]
            [clojure.tools.logging :as log]
            [clojure.walk :as walk]))

(defprotocol Database
  (fetch [this coll k]
    "Find the item based on a key.")
  (fetch-all [this coll]
    "Find all items based on a collection.")
  (find-item [this coll query-m]
    "Find an item matching the query-map.")
  (find-items-by-key-values [this coll k values]
    "Find items whose key 'k' matches one of the given values.")
  (find-items-by-alternatives [this coll value-map-vector]
    "Find items whose properties match properties of at least one of the provided maps.")
  (store! [this coll item]
    "Store the given map and return it.")
  (store-with-id! [this coll key-param item]
    "Store the given map using the value of the kw key-param and return it.")
  (upsert! [this coll query item]
    "Update item that corresponds to query, or if none exist insert item."))

(defn dissoc-id [item]
  (dissoc item :_id))

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

(defrecord MongoDatabase [mongo-db]
  Database

  (fetch [this coll k]
    (when k
      (-> (mcoll/find-map-by-id mongo-db coll k [] true)
          dissoc-id)))

  (fetch-all [this coll]
    (->> (mcoll/find-maps mongo-db coll)
         (map dissoc-id)))

  (find-item [this coll query-m]
    (when query-m
      (-> (mcoll/find-one-as-map mongo-db coll query-m [] true)
          dissoc-id)))

  (find-items-by-key-values [this coll k values]
    (if values
      (let [mongo-key-in-query (key-values->mongo-query-map k values)
            result-m (->> (mcoll/find-maps mongo-db coll mongo-key-in-query)
                          (map dissoc-id))]
        result-m)
      []))

  (find-items-by-alternatives [this coll value-map-vector]
    (if (not-empty value-map-vector)
      (let [mongo-query-map (value-map-vector->or-mongo-query-map value-map-vector)
            result-m (->> (mcoll/find-maps mongo-db coll mongo-query-map)
                          (map dissoc-id))]
        result-m)
      []))

  (store! [this coll item]
    (-> (mcoll/insert-and-return mongo-db coll item)
        (dissoc :_id)))

  (store-with-id! [this coll key-param item]
    (->> (assoc item :_id (get item key-param))
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
(ns mooncake.db.mongo
  (:require [monger.core :as mcore]
            [monger.collection :as mcoll]
            [clojure.tools.logging :as log]))

(defprotocol Database
  (fetch [this coll k keywordise?]
    "Find the item based on a key.")
  (find-item [this coll query-m keywordise?]
    "Find an item matching the query-map.")
  (store! [this coll item]
    "Store the given map and return it.")
  (store-with-id! [this coll key-param item]
    "Store the given map using the value of the kw key-param and return it."))

(defn dissoc-id
  ([item]
   (dissoc-id item true))
  ([item keywordise?]
   (dissoc item (if keywordise? :_id "_id"))))

(defrecord MongoDatabase [mongo-db]
  Database
  (fetch [this coll k keywordise?]
    (when k
      (-> (mcoll/find-map-by-id mongo-db coll k [] keywordise?)
          (dissoc-id keywordise?))))
  (find-item [this coll query-m keywordise?]
    (when query-m
      (-> (mcoll/find-one-as-map mongo-db coll query-m [] keywordise?)
          (dissoc-id keywordise?))))
  (store! [this coll item]
    (-> (mcoll/insert-and-return mongo-db coll item)
        (dissoc :_id)))
  (store-with-id! [this coll key-param item]
    (->> (assoc item :_id (key-param item))
         (store! this coll))))

(defn create-database [mongodb]
  (MongoDatabase. mongodb))

(defn get-mongo-db-and-conn [mongo-uri]
  (log/debug "Connecting to mongo")
  (let [db-and-conn (mcore/connect-via-uri mongo-uri)]
    (log/debug "Connected to mongo.")
    db-and-conn))

(defn get-mongo-db [mongo-uri]
  (:db (get-mongo-db-and-conn mongo-uri)))
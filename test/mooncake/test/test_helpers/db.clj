(ns mooncake.test.test-helpers.db
  (:require [mooncake.db.mongo :as mongo]
            [monger.db :as mdb]
            [monger.core :as m]
            [clojure.walk :as walk])
  (:import (java.util UUID)))

(defn find-item-with-id [data-map coll query-m]
  (some #(when (clojure.set/subset? (set query-m) (set %)) %) (vals (get data-map coll))))

(defn to-vector [value]
  (if (sequential? value)
    value
    [value]))

(defn keywordise [collection keywordise?]
  (if keywordise?
    collection
    (walk/stringify-keys collection)))

(defn find-by-map-query [database coll single-query-map]
  (reduce (fn [result query-key] (let [query-value (get single-query-map query-key)
                                       search-result (mongo/find-items-by-key-values database coll query-key (to-vector query-value))]
                                   (filter (fn [item] (some #{item} search-result)) result)))
          (mongo/fetch-all database coll)
          (keys single-query-map)))

(defrecord MemoryDatabase [data]
  mongo/Database

  (fetch [this coll id]
    (-> (get-in @data [coll id])
        (dissoc :_id)))

  (fetch-all [this coll]
    (->> (vals (get @data coll))
         clojure.walk/keywordize-keys
         (map #(dissoc % :_id))))

  (find-item [this coll query-m]
    (when query-m
      (-> (find-item-with-id @data coll (clojure.walk/keywordize-keys query-m))
          (dissoc :_id))))

  (find-items-by-key-values [this coll k values]
    (-> (for [value values]
          (->> (mongo/fetch-all this coll)
               (filter #(clojure.set/subset? (set {(keyword k) value}) (set %)))))
        flatten
        distinct))

  (find-items-by-alternatives [this coll value-map-vector]
    (reduce (fn [result single-query] (let [query-result (find-by-map-query this coll single-query)]
                                        (distinct (concat result query-result))))
            [] value-map-vector))

  (store! [this coll item]
    (->> (assoc item :_id (UUID/randomUUID))
         (mongo/store-with-id! this coll :_id)))

  (store-with-id! [this coll key-param item]
    (if (mongo/fetch this coll (get item key-param))
      (throw (Exception. "Duplicate ID!"))
      (do
        (swap! data assoc-in [coll (get item key-param)] item)
        (dissoc item :_id))))

  (upsert! [this coll query item]
    (let [id (or (-> (find-item-with-id @data coll query) :_id) (UUID/randomUUID))]
      (swap! data assoc-in [coll id] (assoc item :_id id)))))


(defn create-in-memory-db
  ([] (create-in-memory-db {}))
  ([data] (MemoryDatabase. (atom data))))

(def test-db "mooncake-test")
(def test-db-uri (str "mongodb://localhost:27017/" test-db))

(defn drop-db! []
  (let [{:keys [conn db]} (m/connect-via-uri test-db-uri)]
    (mdb/drop-db db)
    (m/disconnect conn)))

(defn with-mongo-do [thing-to-do]
  (let [{:keys [db conn]} (mongo/get-mongo-db-and-conn test-db-uri)]
    (try (mdb/drop-db db)
         (thing-to-do db)
         (finally (m/disconnect conn)))))

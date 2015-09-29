(ns mooncake.test.test-helpers.db
  (:require [monger.db :as mdb]
            [monger.core :as m]
            [clojure.walk :as walk]
            [clojure.set :as set]
            [mooncake.db.mongo :as mongo])
  (:import (java.util UUID)))

(defn find-item-with-id [data-map coll query-m]
  (some #(when (set/subset? (set query-m) (set %)) %) (vals (get data-map coll))))

(defn to-vector [value]
  (if (sequential? value)
    value
    [value]))

(defn keywordise [collection keywordise?]
  (if keywordise?
    collection
    (walk/stringify-keys collection)))

(defn find-by-map-query [database coll single-query-map keywordise?]
  (reduce (fn [result query-key] (let [query-value (get single-query-map query-key)
                                       search-result (mongo/find-items-by-key-values database coll query-key (to-vector query-value) keywordise?)]
                                   (filter (fn [item] (some #{item} search-result)) result)))
          (keywordise (mongo/fetch-all database coll keywordise?) keywordise?)
          (keys single-query-map)))

(def neutral-comp-fn (constantly 0))

(defn options-m->comp-fn [options-m]
  (if-let [sort-key (first (keys (:sort options-m)))]
    (let [ordering-key (get-in options-m [:sort sort-key])
          ordering-multiplier (get {:ascending 1 :descending -1} ordering-key 0)]
      (fn [value-1 value-2]
        (-> (compare (get value-1 sort-key) (get value-2 sort-key))
            (* ordering-multiplier))))
    neutral-comp-fn))

(defn options-m->batch-fn [options-m]
  (if-let [batch-size (:limit options-m)]
    (partial take batch-size)
    identity))

(defrecord MemoryDatabase [data]
  mongo/Database
  (fetch [this coll id keywordise?]
    (-> (get-in @data [coll id])
        (dissoc :_id)
        (keywordise keywordise?)))

  (fetch-all [this coll keywordise?]
    (keywordise
      (->> (vals (get @data coll))
           (map #(dissoc % :_id)))
      keywordise?))

  (find-item [this coll query-m keywordise?]
    (when query-m
      (-> (find-item-with-id @data coll query-m)
          (dissoc :_id)
          (keywordise keywordise?))))

  (find-items-by-key-values [this coll k values keywordise?]
    (-> (for [value values]
          (->> (mongo/fetch-all this coll keywordise?)
               (filter #(set/subset? (set {k value}) (set %)))))
        flatten
        distinct))

  (find-items-by-alternatives [this coll value-map-vector options-m]
    (when (< 1 (count (keys (:sort options-m)))) (throw (ex-info "Trying to sort by more than one key" (:sort options-m))))
    (let [comp-fn (options-m->comp-fn options-m)
          batch-fn (options-m->batch-fn options-m)]
      (->> value-map-vector
           (map #(find-by-map-query this coll % (not (:stringify? options-m))))
           (apply set/union)
           (sort comp-fn)
           batch-fn)))

  (store! [this coll item]
    (->> (assoc item :_id (UUID/randomUUID))
         (mongo/store-with-id! this coll :_id)))

  (store-with-id! [this coll key-param item]
    (if (mongo/fetch this coll (key-param item) true)
      (throw (Exception. "Duplicate ID!"))
      (do
        (swap! data assoc-in [coll (key-param item)] item)
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

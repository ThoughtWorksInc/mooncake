(ns mooncake.test.test-helpers.db
  (:require [mooncake.db.mongo :as mongo]
            [monger.db :as mdb]
            [monger.core :as m]
            [clojure.walk :as walk])
  (:import (java.util UUID)))

(defn find-item-with-id [data-map coll query-m]
  (some #(when (clojure.set/subset? (set query-m) (set %)) %) (vals (get data-map coll))))

(defrecord MemoryDatabase [data]
  mongo/Database
  (fetch [this coll id keywordise?]
    (-> (get-in @data [coll id])
        (dissoc :_id)))
  (fetch-all [this coll keywordise?]
    (->> (vals (get @data coll))
         (map #(dissoc % :_id))))
  (find-item [this coll query-m keywordise?]
    (when query-m
      (if keywordise?
        (-> (find-item-with-id @data coll query-m)
            (dissoc :_id))
        (-> (mongo/find-item this coll query-m true)
            (walk/stringify-keys)))))
  (find-items-by-key-values [this coll k values keywordise?]
    (->> (for [value values]
           (mongo/find-item this coll {k value} keywordise?))
         (remove nil?)) )
  (store! [this coll item]
    (->> (assoc item :_id (UUID/randomUUID))
         (mongo/store-with-id! this coll :_id)))
  (store-with-id! [this coll key-param item]
    (if (mongo/fetch this coll (key-param item) true)
      (throw (Exception. "Duplicate ID!!"))
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

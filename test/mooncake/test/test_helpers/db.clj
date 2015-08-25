(ns mooncake.test.test-helpers.db
  (:require [mooncake.db.mongo :as mongo])
  (:import (java.util UUID)))

(defrecord MemoryDatabase [data]
  mongo/Database
  (fetch [this coll id]
    (-> (get-in @data [coll id])
        (dissoc :_id)))
  (find-item [this coll query-m]
    (-> (some #(when (clojure.set/subset? (set query-m) (set %)) %) (vals (get @data coll)))
        (dissoc :_id)))
  (store! [this coll item]
    (->> (assoc item :_id (UUID/randomUUID))
         (mongo/store-with-id! this coll :_id)))
  (store-with-id! [this coll key-param item]
    (do
      (swap! data assoc-in [coll (key-param item)] item)
      (dissoc item :_id))))

(defn create-in-memory-db
  ([] (create-in-memory-db {}))
  ([data] (MemoryDatabase. (atom data))))

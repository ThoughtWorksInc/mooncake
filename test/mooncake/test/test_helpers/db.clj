(ns mooncake.test.test-helpers.db
  (:require [mooncake.db.mongo :as mongo]))

(defrecord MemoryDatabase [data]
  mongo/Database
  (fetch [this coll k]
    (get-in @data [coll k]))
  (find-item [this coll query-m]
    (some #(when (clojure.set/subset? (set query-m) (set %)) %) (vals (get @data coll))))
  (store! [this coll key-param item]
    (do
      (swap! data assoc-in [coll (key-param item)] item)
      item)))

(defn create-in-memory-db
  ([] (create-in-memory-db {}))
  ([data] (MemoryDatabase. (atom data))))

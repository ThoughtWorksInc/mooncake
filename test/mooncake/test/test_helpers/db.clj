(ns mooncake.test.test-helpers.db
  (:require [mooncake.db.mongo :as mongo]))

(defrecord MemoryDatabase [data]
  mongo/Database
  (fetch [this coll k]
    (@data k))
  (find-item [this coll query-m]
    (some #(when (clojure.set/subset? (set query-m) (set %)) %) (vals @data)))
  (store! [this coll key-param item]
    (do
      (swap! data assoc (key-param item) item)
      item)))

(defn create-in-memory-db
  ([] (create-in-memory-db {}))
  ([data] (MemoryDatabase. (atom data))))

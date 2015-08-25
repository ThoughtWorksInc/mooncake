(ns mooncake.db.activity
  (:require [mooncake.db.mongo :as mongo]))

(def collection "activity")

(defn store-activity! [db activity]
  (mongo/store! db collection activity))
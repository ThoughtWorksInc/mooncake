(ns mooncake.db.mongo
  (:require [monger.core :as mcore]
            [monger.collection :as mcoll]
            [clojure.tools.logging :as log]))

(defprotocol Database
  (fetch [this k]
         "Find the item based on a key.")
  (store! [this key-param item]
          "Store the given map using the value of the kw key-param and return it."))
   
(defrecord MongoDatabase [mongo-db coll]
  Database
  (fetch [this k]
    (when k
      (-> (mcoll/find-map-by-id mongo-db coll k)
          (dissoc :_id))))
  (store! [this key-param item]
    (-> (mcoll/insert-and-return mongo-db coll (assoc item :_id (key-param item)))
        (dissoc :_id))))

(defn create-mongo-store [mongodb collection-name]
  (MongoDatabase. mongodb collection-name))

(defn get-mongo-db [mongo-uri]
  (log/debug "Connecting to mongo")
  (let [db (-> (mcore/connect-via-uri mongo-uri) :db)]
    (log/debug "Connected to mongo.")
    db))

(defn create-user! [db auth-provider-user-id username]
  (let [user {:auth-provider-user-id auth-provider-user-id
              :username username}]
    (store! db :auth-provider-user-id user)))

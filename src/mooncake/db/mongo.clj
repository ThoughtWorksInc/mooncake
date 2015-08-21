(ns mooncake.db.mongo
  (:require [monger.core :as mcore]
            [monger.collection :as mcoll]
            [clojure.tools.logging :as log]))

(defprotocol Database
  (fetch [this k]
         "Find the item based on a key.")
  (find-item [this query-m]
             "Find an item matching the query-map.")
  (store! [this key-param item]
          "Store the given map using the value of the kw key-param and return it."))
   
(defrecord MongoDatabase [mongo-db coll]
  Database
  (fetch [this k]
    (when k
      (-> (mcoll/find-map-by-id mongo-db coll k)
          (dissoc :_id))))
  (find-item [this query-m]
    (when query-m
      (-> (mcoll/find-one-as-map mongo-db coll query-m)
          (dissoc :_id))))
  (store! [this key-param item]
    (-> (mcoll/insert-and-return mongo-db coll (assoc item :_id (key-param item)))
        (dissoc :_id))))

(defn create-mongo-store [mongodb collection-name]
  (MongoDatabase. mongodb collection-name))

(defn get-mongo-db-and-conn [mongo-uri]
  (log/debug "Connecting to mongo")
  (let [db-and-conn (mcore/connect-via-uri mongo-uri)]
    (log/debug "Connected to mongo.")
    db-and-conn))

(defn get-mongo-db [mongo-uri]
  (:db (get-mongo-db-and-conn mongo-uri)))

(defn with-mongo-do [mongo-uri thing-to-do]
  (let [{:keys [db conn]} (get-mongo-db-and-conn mongo-uri)] 
    (try (thing-to-do db)
         (finally (mcore/disconnect conn)))))

(defn create-user! [db auth-provider-user-id username]
  (let [user {:auth-provider-user-id auth-provider-user-id
              :username username}]
    (store! db :auth-provider-user-id user)))

(ns mooncake.db.mongo
  (:require [monger.collection :as mc]
            [monger.core :as mongo]
            [clojure.tools.logging :as log]))

(defprotocol database
  (fetch [this user-id]))


(defn get-mongo-db [mongo-uri]
  (log/debug "Connecting to mongo")
  (let [db (-> (mongo/connect-via-uri mongo-uri) :db)]
    (log/debug "Connected to mongo.")
    db))

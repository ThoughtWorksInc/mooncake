(ns mooncake.db.user
  (:require [mooncake.db.mongo :as mongo]))

(def collection "user")

(defn create-user! [db auth-provider-user-id username]
  (let [user {:auth-provider-user-id auth-provider-user-id
              :username username}]
    (mongo/store! db collection :auth-provider-user-id user)))

(defn find-user [db username]
  (mongo/find-item db collection {:username username}))

(defn fetch-user [db auth-provider-user-id]
  (mongo/fetch db collection auth-provider-user-id))

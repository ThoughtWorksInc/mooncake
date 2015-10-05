(ns mooncake.db.user
  (:require [mooncake.db.mongo :as mongo]))

(def collection "user")

(defn create-user! [store auth-provider-user-id username]
  (let [user {:auth-provider-user-id auth-provider-user-id
              :username username}]
    (mongo/store-with-id! store collection :auth-provider-user-id user)))

(defn find-user [store username]
  (mongo/find-item store collection {:username username} {:stringify? false}))

(defn fetch-user [store auth-provider-user-id]
  (mongo/fetch store collection auth-provider-user-id {:stringify? false}))

(defn update-feed-settings! [store username feed-settings]
  (mongo/upsert! store collection {:username username} :feed-settings feed-settings))


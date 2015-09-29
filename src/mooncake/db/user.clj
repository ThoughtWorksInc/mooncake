(ns mooncake.db.user
  (:require [mooncake.db.mongo :as mongo]))

(def collection "user")

(defn create-user! [db auth-provider-user-id username]
  (let [user {:auth-provider-user-id auth-provider-user-id
              :username username}]
    (mongo/store-with-id! db collection :auth-provider-user-id user)))

(defn find-user [db username]
  (mongo/find-item db collection {:username username} true))

(defn fetch-user [db auth-provider-user-id]
  (mongo/fetch db collection auth-provider-user-id {:stringify? false}))

(defn update-feed-settings! [db username feed-settings]
  (let [user (find-user db username)
        user-with-feed-settings (assoc user :feed-settings feed-settings)]
      (mongo/upsert! db collection {:username username} user-with-feed-settings)))


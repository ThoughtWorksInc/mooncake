(ns mooncake.db.user
  (:require [mooncake.db.mongo :as mongo]))

(defn create-user! [db auth-provider-user-id username]
  (let [user {:auth-provider-user-id auth-provider-user-id
              :username username}]
    (mongo/store! db :auth-provider-user-id user)))

(defn find-user [user-store username]
  (mongo/find-item user-store {:username username}))

(ns mooncake.controller.create-account
  (:require
    [mooncake.helper :as mh]
    [mooncake.db.mongo :as mongo]
    [mooncake.view.create-account :as cav]))

(defn show-create-account [request]
  (if (mh/authenticated? request)
    (mh/enlive-response (cav/create-account request) (:context request))  
    (mh/redirect-to request :sign-in)))

(defn create-account [db request]
  (if (mh/authenticated? request) 
    (if-let [username (get-in request [:params :username]) ] 
      (let [auth-provider-user-id (get-in request [:session :auth-provider-user-id])]
        (mongo/create-user! db auth-provider-user-id username)
        (mh/redirect-to request :sign-in))
      (show-create-account request))
    (mh/redirect-to request :sign-in)))

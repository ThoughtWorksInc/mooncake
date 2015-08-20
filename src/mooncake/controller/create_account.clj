(ns mooncake.controller.create-account
  (:require
    [mooncake.helper :as mh]
    [mooncake.db.mongo :as mongo]
    [mooncake.view.create-account :as cav]))

(defn show-create-account [request]
  (if (mh/authenticated? request)
    (mh/enlive-response (cav/create-account request) (:context request))  
    (mh/redirect-to request :sign-in)))

(defn- account-created-response [request user]
  (let [updated-session (-> (:session request)
                            (dissoc :auth-provider-user-id)
                            (assoc :username (:username user)))]
    (-> (mh/redirect-to request :index)
        (assoc :session updated-session))))

(defn create-account [user-store request]
  (if (mh/authenticated? request) 
    (if-let [username (get-in request [:params :username]) ] 
      (let [auth-provider-user-id (get-in request [:session :auth-provider-user-id])
            created-user (mongo/create-user! user-store auth-provider-user-id username)]
        (account-created-response request created-user))
      (show-create-account request))
    (mh/redirect-to request :sign-in)))

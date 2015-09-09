(ns mooncake.controller.customise-feed
  (:require [mooncake.db.user :as user])
  )

(defn customise-feed [db request]
  (let [username (get-in request [:session :username])
        activity-sources (get-in request [:context :activity-sources])
        post-parameters (:params request)
        feed-settings (zipmap (keys post-parameters) (repeat true))]
    (user/update-feed-settings! db username feed-settings)))

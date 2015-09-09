(ns mooncake.controller.customise-feed
  (:require [mooncake.db.user :as user])
  )

(defn customise-feed [db request]
  (let [username (get-in request [:session :username])
        activity-source-keys (keys (get-in request [:context :activity-sources]))
        posted-activity-source-keys (keys (select-keys (:params request) activity-source-keys))
        feed-settings-false (zipmap activity-source-keys (repeat false))
        feed-settings-submitted (zipmap posted-activity-source-keys (repeat true))
        feed-settings-combined (merge feed-settings-false feed-settings-submitted)]
    (user/update-feed-settings! db username feed-settings-combined)))

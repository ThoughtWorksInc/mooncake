(ns mooncake.controller.feed
  (:require [mooncake.activity :as a]
            [mooncake.helper :as mh]
            [mooncake.view.feed :as f]))

(defn feed [database request]
  (let [activities (a/retrieve-activities-from-database database)]
    (mh/enlive-response (f/feed (assoc-in request [:context :activities] activities)) (:context request))))

(ns mooncake.view.create-account
  (:require [mooncake.view.view-helpers :as vh]))

(defn create-account [request]
  (vh/load-template "public/create-account.html"))

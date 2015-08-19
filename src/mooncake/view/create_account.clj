(ns mooncake.view.create-account
  (:require [net.cgrand.enlive-html :as html]
            [mooncake.routes :as r]
            [mooncake.view.view-helpers :as vh]))

(defn set-form-action [enlive-m]
  (html/at enlive-m [:.clj--create-account__form] (html/set-attr :action (r/path :create-account))))

(defn create-account [request]
  (-> (vh/load-template "public/create-account.html")
      set-form-action))

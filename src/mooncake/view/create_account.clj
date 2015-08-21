(ns mooncake.view.create-account
  (:require [net.cgrand.enlive-html :as html]
            [mooncake.routes :as r]
            [mooncake.view.view-helpers :as vh]))

(defn set-form-action [enlive-m]
  (html/at enlive-m [:.clj--create-account__form] (html/set-attr :action (r/path :create-account))))

(defn remove-username-validation [enlive-m error-m]
  (if-let [error-key (:username error-m)] 
    (html/at enlive-m
             [:.clj--username] (html/add-class "form-row--validation-error")
             [:.clj--username__validation] (html/set-attr :data-l8n "content:create-account/username-blank-validation-message")
             ) 
    (vh/remove-element enlive-m [:.clj--username__validation])))

(defn create-account [request]
  (-> (vh/load-template "public/create-account.html")
      (remove-username-validation (get-in request [:context :error-m]))
      set-form-action))

(ns mooncake.view.create-account
  (:require [net.cgrand.enlive-html :as html]
            [mooncake.routes :as r]
            [mooncake.view.view-helpers :as vh]
            [mooncake.translation :as translation]))

(defn username-validation-message-translation [error-key]
  (case error-key
    :blank          "content:create-account/username-blank-validation-message"
    :too-long       "content:create-account/username-too-long-validation-message"
    :invalid-format "content:create-account/username-invalid-format-validation-message"
    :duplicate      "content:create-account/username-duplicate-validation-message"
                    "content:create-account/username-default-validation-message"))

(defn set-form-action [enlive-m]
  (html/at enlive-m [:.clj--create-account__form] (html/set-attr :action (r/path :create-account))))

(defn username-validation [enlive-m error-m]
  (if-let [error-key (:username error-m)]
    (html/at enlive-m
             [:.clj--username] (html/add-class "form-row--validation-error")
             [:.clj--username__validation] (html/set-attr :data-l8n (username-validation-message-translation error-key)))
    (vh/remove-element enlive-m [:.clj--username__validation])))

(defn repopulate-fields [enlive-m params]
  (if-let [username (:username params)]
    (html/at enlive-m
             [:.clj--username__input] (html/set-attr :value username))
    enlive-m))

(defn create-account [request]
  (-> (vh/load-template-with-lang "public/create-account.html" (translation/get-locale-from-request request))
      (username-validation (get-in request [:context :error-m]))
      (repopulate-fields (get-in request [:context :params]))
      set-form-action))

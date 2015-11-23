(ns mooncake.view.sign-in
  (:require [net.cgrand.enlive-html :as html]
            [mooncake.routes :as routes]
            [mooncake.view.view-helpers :as vh]
            [mooncake.translation :as translation]))

(defn set-sign-in-link [enlive-m path]
  (html/at enlive-m
           [:.clj--sign-in-with-d-cent] (html/set-attr :href path)))

(defn set-flash-message [enlive-m request]
  (if-not (get-in request [:flash :sign-in-failed])
    (vh/remove-element enlive-m [:.clj--flash-message-container])
    enlive-m))

(defn sign-in [request]
  (-> (vh/load-template "public/sign-in.html")
      (set-sign-in-link (routes/path :stonecutter-sign-in))
      (vh/update-language (translation/get-locale-from-request request))
      (set-flash-message request)))

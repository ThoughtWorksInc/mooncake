(ns mooncake.view.sign-in
  (:require [net.cgrand.enlive-html :as html]
            [mooncake.helper :as h]
            [mooncake.routes :as routes]
            [mooncake.view.view-helpers :as vh]))

(defn set-sign-in-link [enlive-m path]
  (html/at enlive-m
           [:.clj--sign-in-with-d-cent] (html/set-attr :href path)))

(defn sign-in [request]
  (-> (vh/load-template "public/sign-in.html")
      (set-sign-in-link (routes/path :stonecutter-sign-in))))

(ns mooncake.view.sign-in
  (:require [net.cgrand.enlive-html :as html]
            [mooncake.helper :as h]
            [mooncake.view.view-helpers :as vh]))

(defn sign-in [request]
  (vh/load-template "public/sign-in.html"))

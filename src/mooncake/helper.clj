(ns mooncake.helper
  (:require [ring.util.response :as r]
            [net.cgrand.enlive-html :as html]
            [mooncake.translation :as t]
            [mooncake.view.view-helpers :as vh]))

(defn enlive-response [enlive-m context]
  (-> enlive-m
      (t/context-translate context)
      vh/enlive-to-str
      r/response
      (r/content-type "text/html")))


(ns mooncake.helper
  (:require [ring.util.response :as r]
            [net.cgrand.enlive-html :as html]
            [clojure.contrib.humanize :as h]
            [clj-time.core :as c]
            [clj-time.format :as f]
            [mooncake.translation :as t]
            [mooncake.view.view-helpers :as vh]))

(defn enlive-response [enlive-m context]
  (-> enlive-m
      (t/context-translate context)
      vh/enlive-to-str
      r/response
      (r/content-type "text/html")))

(defn datetime-str->datetime [datetime-str]
  (f/parse (f/formatters :date-time) datetime-str))

(defn humanise-time [datetime-str]
  (-> (datetime-str->datetime datetime-str)
      h/datetime))

(def after? c/after?)

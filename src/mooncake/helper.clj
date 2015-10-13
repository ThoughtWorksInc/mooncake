(ns mooncake.helper
  (:require [ring.util.response :as r]
            [clojure.contrib.humanize :as h]
            [clj-time.core :as c]
            [clj-time.format :as f]
            [mooncake.routes :as routes]
            [mooncake.translation :as t]
            [mooncake.view.view-helpers :as vh]))

(defn signed-in? [request]
  (boolean (get-in request [:session :username])))

(defn authenticated? [request]
  (boolean (get-in request [:session :auth-provider-user-id])))

(defn enlive-response [enlive-m context]
  (-> enlive-m
      vh/add-logo-link
      (vh/remove-elements [:.clj--STRIP])
      vh/add-anti-forgery
      (t/context-translate context)
      vh/enlive-to-str
      r/response
      (r/content-type "text/html")))

(defn datetime-str->datetime [datetime-str]
  (f/parse (f/formatters :date-time-parser) datetime-str))

(defn humanise-time [datetime-str]
  (-> (datetime-str->datetime datetime-str)
      h/datetime))

(def after? c/after?)

(defn request->config-m [request]
  (get-in request [:context :config-m]))

(defn redirect-to [request route-key]
  (r/redirect (routes/absolute-path (request->config-m request) route-key)))

(defn map-over-values [m f]
  (into {} (for [[k v] m] [k (f v)])))

(defn has-keys? [m ks]
  (= (set ks) (set (keys (select-keys m ks)))))
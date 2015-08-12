(ns mooncake.view.view-helpers
  (:require [net.cgrand.enlive-html :as html]
            [net.cgrand.jsoup :as jsoup]
            [clojure.tools.logging :as log]))

(def template-caching? (atom true))

(def template-cache (atom {}))

(defn html-resource-with-log [path]
  (log/debug (format "Loading template %s from file" path))
  (html/html-resource path {:parser jsoup/parser}))

(defn load-template [path]
  (if @template-caching?
    (if (contains? @template-cache path)
      (get @template-cache path)
      (let [html (html-resource-with-log path)]
        (swap! template-cache #(assoc % path html))
        html))
    (html-resource-with-log path)))

(defn enlive-to-str [nodes]
  (->> nodes
       html/emit*
       (apply str)))

(defn remove-elements [enlive-m selector]
  (html/at enlive-m selector nil))

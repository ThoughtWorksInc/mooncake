(ns mooncake.jstest.macros
  (:require [clojure.java.io :as io]
            [mooncake.view.feed :as feed-view]
            [mooncake.view.view-helpers :as vh]
            [net.cgrand.enlive-html :as html]))

(defmacro load-template [r]
  "Reads and returns a template as a string."
  (slurp (io/resource r)))

(defmacro type-key []
  (keyword "@type"))

(defmacro generate-test-html-data [data-map]
  (let [enlive-m (vh/load-template "public/feed.html")
        activity-sources {}
        transformed-enlive (feed-view/generate-activity-stream-items enlive-m (:activities (eval data-map)) activity-sources)]
    (vh/enlive-to-str transformed-enlive)))

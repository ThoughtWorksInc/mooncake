(ns mooncake.jstest.macros
  (:require [clojure.java.io :as io]
            [mooncake.view.feed :as feed-view]
            [mooncake.view.view-helpers :as vh]))

(defmacro load-template [r]
  "Reads and returns a template as a string."
  (slurp (io/resource r)))

(defmacro generate-test-html-data [data-map]
  (let [activity-sources {}
        request {:context {:activities (eval data-map) :activity-sources activity-sources :hide-activities? false}}
        transformed-enlive (feed-view/feed-fragment request)]
    (vh/enlive-to-str transformed-enlive)))

(defmacro generate-test-html-data-hidden [data-map]
  (let [activity-sources {}
        request {:context {:activities (eval data-map) :activity-sources activity-sources :hide-activities? true}}
        transformed-enlive (feed-view/feed-fragment request)]
    (vh/enlive-to-str transformed-enlive)))

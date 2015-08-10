(ns mooncake.activity
  (:require [clj-http.client :as http]
            [clj-yaml.core :as yaml]
            [clojure.java.io :as io]
            [clojure.tools.logging :as log]
            [mooncake.helper :as mh]))


(defn load-activity-sources [activity-resource-name]
  (-> activity-resource-name
      io/resource
      slurp
      yaml/parse-string))

(def activity-sources
  (load-activity-sources "activity-sources.yml"))

(defn get-json-from-activity-source [url]
  (try
    (:body (http/get url {:accept :json :as :json-string-keys}))
    (catch Exception e
      (log/warn (str "Unable to retrieve activities from " url " --- " e))
      nil)))

(defn retrieve-activities-from-source [source-k-v-pair]
  (let [[source-key source-url] source-k-v-pair
        activities (get-json-from-activity-source source-url)]
    (map #(assoc % :activity-src source-key) activities)))

(defn retrieve-activities [activity-sources]
  (let [published-time (fn [activity]
                         (mh/datetime-str->datetime (get activity "published")))]
    (->> (map retrieve-activities-from-source activity-sources)
         flatten
         (sort-by published-time mh/after?))))

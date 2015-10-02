(ns mooncake.activity
  (:require [clj-http.client :as http]
            [clj-yaml.core :as yaml]
            [clojure.java.io :as io]
            [clojure.tools.logging :as log]
            [mooncake.db.activity :as adb]
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

(defn sort-by-published-time [activities]
  (let [published-time (fn [activity]
                         (mh/datetime-str->datetime (get activity "published")))]
    (->> activities
         (sort-by published-time mh/after?))))

(defn retrieve-activities-from-source [source-k-v-pair]
  (let [[source-key source-attributes] source-k-v-pair
        activities (get-json-from-activity-source (:url source-attributes))]
    (map #(assoc % "activity-src" source-key) activities)))

(defn poll-activity-sources [activity-sources]
  (->> activity-sources
       (map retrieve-activities-from-source)
       flatten
       sort-by-published-time))

(defn retrieve-activities [database activity-source-keys]
  (adb/fetch-activities-by-activity-sources-and-types database activity-source-keys))

(defn sync-activities! [db activity-sources]
  (let [activities (poll-activity-sources activity-sources)]
    (doall (map (partial adb/store-activity! db) (reverse activities)))))

(defn sync-activities-task [db activity-sources]
  (fn [time]
    (log/debug (format "Syncing activities at %s" time))
    (sync-activities! db activity-sources)))

(defn retrieve-activity-types [db]
  (adb/fetch-activity-types db))


(ns mooncake.activity
  (:require [clj-http.client :as http]
            [clj-yaml.core :as yaml]
            [clojure.java.io :as io]
            [clojure.tools.logging :as log]
            [clj-time.coerce :as time-coerce]
            [mooncake.db.activity :as adb]
            [mooncake.helper :as mh]
            [mooncake.domain.activity :as activity]))

(defn load-activity-sources [activity-resource-name]
  (-> activity-resource-name
      io/resource
      slurp
      yaml/parse-string))

(def activity-sources
  (load-activity-sources "activity-sources.yml"))

(defn get-json-from-activity-source [url query-params]
  (try
    (let [query-map (if query-params {:accept :json :as :json :query-params query-params}
                                     {:accept :json :as :json})]
      (:body (http/get url query-map)))
    (catch Exception e
      (log/warn (str "Unable to retrieve activities from " url " --- " e))
      nil)))

(defn sort-by-published-time [activities]
  (let [published-time (fn [activity]
                         (mh/datetime-str->datetime (activity/activity->published activity)))]
    (->> activities
         (sort-by published-time mh/after?))))

(defn retrieve-activities-from-source [store source-k-v-pair]
  (let [[source-key source-attributes] source-k-v-pair
        most-recent-activity-date (time-coerce/to-string (adb/fetch-most-recent-activity-date store source-key))
        activities (get-json-from-activity-source (:url source-attributes)
                                                  (when most-recent-activity-date {:from most-recent-activity-date}))]
    (map #(assoc % :activity-src source-key) activities)))

(defn poll-activity-sources [store activity-sources]
  (->> activity-sources
       (map (partial retrieve-activities-from-source store))
       flatten
       sort-by-published-time))

(defn retrieve-activities [store activity-source-keys]
  (adb/fetch-activities-by-activity-sources-and-types store activity-source-keys))

(defn sync-activities! [store activity-sources]
  (let [activities (poll-activity-sources store activity-sources)]
    (doall (map (partial adb/store-activity! store) (reverse activities)))))

(defn sync-activities-task [store activity-sources]
  (fn [time]
    (log/debug (format "Syncing activities at %s" time))
    (sync-activities! store activity-sources)))

(defn retrieve-activity-types [store]
  (adb/fetch-activity-types store))


(ns mooncake.activity
  (:require [clj-http.client :as http]
            [clj-yaml.core :as yaml]
            [clojure.java.io :as io]
            [clojure.tools.logging :as log]
            [clj-time.coerce :as time-coerce]
            [cheshire.core :as json]
            [mooncake.db.activity :as adb]
            [mooncake.helper :as mh]
            [mooncake.domain.activity :as activity])
  (:import [org.jose4j.jwk JsonWebKeySet VerificationJwkSelector]
           [org.jose4j.jws JsonWebSignature]))

(defn load-activity-sources [activity-resource-name]
  (-> activity-resource-name
      io/resource
      slurp
      yaml/parse-string))

(def activity-sources
  (load-activity-sources "activity-sources.yml"))

(defn verify-and-return-payload [json-web-key-set-url jws]
  (let [jwk-set-response (http/get json-web-key-set-url {:accept :json})
        json-web-key-set (JsonWebKeySet. (:body jwk-set-response))
        jwk (.select (VerificationJwkSelector.) jws (.getJsonWebKeys json-web-key-set))
        json-payload (.getPayload (doto jws (.setKey (.getKey jwk))))]
    json-payload))

(defn verify-and-return-activities [json-web-key-set-url jws]
  (let [json-payload (verify-and-return-payload json-web-key-set-url jws)
        activities (json/parse-string json-payload true)]
    (map #(assoc % :signed true) activities)))

(defn handle-unsigned-activity-source-response [unsigned-activity-source-response]
  (->> unsigned-activity-source-response
       :body
       (map #(assoc % :signed false))))

(defn handle-signed-activity-source-response [signed-activity-source-response]
  (let [json-web-key-set-url (get-in signed-activity-source-response [:body :jku])
        json-web-key-set-signed-payload (get-in signed-activity-source-response [:body :jws-signed-payload])
        jws (doto (JsonWebSignature.) (.setCompactSerialization json-web-key-set-signed-payload))]
    (try
      (verify-and-return-activities json-web-key-set-url jws)
      (catch Exception e
        (log/warn (str "Verification of signed activity response failed - attempting to return unsigned activities ----" e))
        (json/parse-string (.getUnverifiedPayload jws) true)))))

(defn is-signed-response? [activity-source-response]
  (and (get-in activity-source-response [:body :jws-signed-payload])
       (get-in activity-source-response [:body :jku])))

(defn get-json-from-activity-source [url query-params]
  (try
    (let [query-map (if query-params {:accept :json :as :json :query-params query-params}
                                     {:accept :json :as :json})
          activity-source-response (http/get url query-map)]
      (if (is-signed-response? activity-source-response)
        (handle-signed-activity-source-response activity-source-response)
        (handle-unsigned-activity-source-response activity-source-response)))
    (catch Exception e
      (log/warn (str "Unable to retrieve activities from " url " --- " e))
      nil)))

(defn sort-by-published-time [activities]
  (let [published-time (fn [activity]
                         (mh/datetime-str->datetime (activity/activity->published activity)))]
    (->> activities
         (sort-by published-time mh/after?))))

(defn log-invalid-activity [invalid-activity]
  (log/warn "Invalid format: " invalid-activity))

(defn validate-activity-fn [activity]
  (if (mh/has-keys? activity activity/required-activity-attributes)
    true
    (do (log-invalid-activity activity)
        false)))

(defn validate-activities [activities]
  (filter validate-activity-fn activities))

(defn retrieve-activities-from-source [store source-k-v-pair]
  (let [[source-key source-attributes] source-k-v-pair
        most-recent-activity-date (time-coerce/to-string (adb/fetch-most-recent-activity-date store source-key))
        query-params (when most-recent-activity-date {:from most-recent-activity-date})
        activities (get-json-from-activity-source (:url source-attributes) query-params)]
    (map #(assoc % :activity-src source-key) activities)))

(defn poll-activity-sources [store activity-sources]
  (->> activity-sources
       (map (partial retrieve-activities-from-source store))
       flatten
       sort-by-published-time))

(defn retrieve-activities [store activity-source-keys params]
  (adb/fetch-activities-by-activity-sources-and-types store activity-source-keys params))

(defn sync-activities! [store activity-sources]
  (let [activities (poll-activity-sources store activity-sources)]
    (doall (map (partial adb/store-activity! store) (reverse activities)))))

(defn sync-activities-task [store activity-sources]
  (fn [time]
    (log/debug (format "Syncing activities at %s" time))
    (sync-activities! store activity-sources)))

(defn retrieve-activity-types [store]
  (adb/fetch-activity-types store))

(defn total-count-by-feed [store activity-source-keys]
  (adb/fetch-total-count-by-sources-and-types store activity-source-keys))


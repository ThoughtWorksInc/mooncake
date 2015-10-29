(ns mooncake.activity-updater
  (:require [mooncake.validation :as validation]
            [clj-time.coerce :as time-coerce]
            [mooncake.db.activity :as adb]
            [mooncake.domain.activity :as domain]
            [mooncake.activity :as a]
            [mooncake.helper :as mh]
            [clojure.tools.logging :as log]
            [cheshire.core :as json]
            [clj-http.client :as http])
  (:import (org.jose4j.jwk VerificationJwkSelector JsonWebKeySet)
           (org.jose4j.jws JsonWebSignature)))

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

(defn- handle-unsigned-activity-source-response [unsigned-activity-source-response]
  (->> unsigned-activity-source-response
       :body
       (map #(assoc % :signed false))))

(defn- handle-signed-activity-source-response [signed-activity-source-response]
  (let [json-web-key-set-url (get-in signed-activity-source-response [:body :jku])
        json-web-key-set-signed-payload (get-in signed-activity-source-response [:body :jws-signed-payload])
        jws (doto (JsonWebSignature.) (.setCompactSerialization json-web-key-set-signed-payload))]
    (try
      (verify-and-return-activities json-web-key-set-url jws)
      (catch Exception e
        (log/warn (str "Verification of signed activity response failed - attempting to return unsigned activities ----" e))
        (map #(assoc % :signed :verification-failed) (json/parse-string (.getUnverifiedPayload jws) true))))))

(defn get-json-from-activity-source [url query-params]
  (try
    (let [query-map (if query-params {:accept :json :as :json :query-params query-params}
                                     {:accept :json :as :json})
          activity-source-response (http/get url query-map)]
      (if (a/is-signed-response? activity-source-response)
        (handle-signed-activity-source-response activity-source-response)
        (handle-unsigned-activity-source-response activity-source-response)))
    (catch Exception e
      (log/warn (str "Unable to retrieve activities from " url " --- " e))
      nil)))

(defn- sort-by-published-time [activities]
  (let [published-time (fn [activity]
                         (mh/datetime-str->datetime (domain/activity->published activity)))]
    (->> activities
         (sort-by published-time mh/after?))))

(defn log-invalid-activity [invalid-activity activity-error-m]
  (log/warn (str "Invalid activity: " invalid-activity))
  (doseq [[attr error] activity-error-m] (log/warn (str "Attribute " attr " is " error))))


(defn- validate-activity-fn [activity]
  (if-let [activity-error-m (validation/validate-activity activity)]
    (do (log-invalid-activity activity activity-error-m)
        false)
    true))

(defn remove-invalid-activities [activities]
  (filter validate-activity-fn activities))

(defn retrieve-activities-from-source [store source-k-v-pair]
  (let [[source-key source-attributes] source-k-v-pair
        most-recent-activity-date (time-coerce/to-string (adb/fetch-most-recent-activity-date store source-key))
        query-params (when most-recent-activity-date {:from most-recent-activity-date})
        activities (get-json-from-activity-source (:url source-attributes) query-params)]
    (->> activities
         remove-invalid-activities
         (map #(assoc % :activity-src source-key)))))

(defn poll-activity-sources [store activity-sources]
  (->> activity-sources
       (map (partial retrieve-activities-from-source store))
       flatten
       sort-by-published-time))

(defn sync-activities! [store activity-sources]
  (let [activities (poll-activity-sources store activity-sources)]
    (doall (map (partial adb/store-activity! store) (reverse activities)))))

(defn sync-activities-task [store activity-sources]
  (fn [time]
    (log/debug (format "Syncing activities at %s" time))
    (sync-activities! store activity-sources)))
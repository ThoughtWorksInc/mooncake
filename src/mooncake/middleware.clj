(ns mooncake.middleware
  (:require [ring.util.response :as r]
            [clojure.tools.logging :as log]
            [mooncake.db.activity :as adb]
            [mooncake.helper :as mh]
            [mooncake.translation :as translation]))

(defn wrap-config [handler config-m]
  (fn [request]
    (-> request
        (assoc-in [:context :config-m] config-m)
        handler)))

(defn generate-activity-sources-with-types [activity-sources activity-types]
  (reduce
    (fn [generated-m [activity-src-key activity-src-data]]
      (let [activity-types-for-src (get activity-types (name activity-src-key))
            activity-src-data-with-types (assoc activity-src-data :activity-types activity-types-for-src)]
        (assoc generated-m activity-src-key activity-src-data-with-types)))
    {}
    activity-sources))

(defn wrap-activity-sources-and-types [store activity-sources handler]
  (fn [request]
    (let [activity-types (adb/fetch-activity-types store)
          activity-sources-with-types (generate-activity-sources-with-types activity-sources activity-types)]
      (handler (assoc-in request [:context :activity-sources] activity-sources-with-types)))))

(defn wrap-error-handling [handler err-handler]
  (fn [request]
    (try
      (handler request)
      (catch Exception e
        (log/error e e)
        (err-handler request)))))

(defn wrap-handle-403 [handler error-403-handler]
  (fn [request]
    (let [response (handler request)]
      (if (= (:status response) 403)
        (error-403-handler request)
        response))))

(defn wrap-signed-in [handler sign-in-route]
  (fn [request]
    (if (mh/signed-in? request)
      (handler request)
      (r/redirect sign-in-route))))

(defn wrap-handlers-excluding [handlers wrap-function exclusions]
  (into {} (for [[k v] handlers]
             [k (if (k exclusions) v (wrap-function v))])))

(defn wrap-just-these-handlers [handlers-m wrap-function inclusions]
  (into {} (for [[k v] handlers-m]
             [k (if (k inclusions) (wrap-function v) v)])))



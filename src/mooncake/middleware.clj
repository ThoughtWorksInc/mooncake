(ns mooncake.middleware
  (:require [ring.util.response :as r]
            [mooncake.helper :as mh]
            [mooncake.translation :as translation]))

(defn wrap-translator [handler]
  (fn [request]
    (-> request
        (assoc-in [:context :translator] (translation/translations-fn translation/translation-map))
        handler)))

(defn wrap-config [handler config-m]
  (fn [request]
    (-> request
        (assoc-in [:context :config-m] config-m)
        handler)))

(defn wrap-activity-sources [handler activity-sources]
  (fn [request]
    (-> request
        (assoc-in [:context :activity-sources] activity-sources)
        handler)))

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


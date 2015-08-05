(ns mooncake.middleware
  (:require [mooncake.translation :as translation]))

(defn wrap-translator [handler]
  (fn [request]
    (-> request
        (assoc-in [:context :translator] (translation/translations-fn translation/translation-map))
        handler)))

(defn wrap-handle-404 [handler error-404-handler]
  (fn [request]
    (let [response (handler request)]
      (if (= (:status response) 404)
        (error-404-handler request)
        response))))

(defn wrap-handlers [handlers wrap-function exclusions]
  (into {} (for [[k v] handlers]
             [k (if (k exclusions) v (wrap-function v))])))


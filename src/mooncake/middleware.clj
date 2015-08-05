(ns mooncake.middleware
  (:require [mooncake.translation :as translation]))

(defn wrap-translator [handler]
  (fn [request]
    (-> request
        (assoc-in [:context :translator] (translation/translations-fn translation/translation-map))
        handler)))

(ns mooncake.view.error
  (:require [net.cgrand.enlive-html :as html]
            [mooncake.view.view-helpers :as vh]
            [mooncake.translation :as translation]))

(defn modify-error-translation-keys [enlive-map error-page-key]
  (html/at enlive-map
           [:body] (html/set-attr :class (str "func--" error-page-key "-page"))
           [:title] (html/set-attr :data-l8n (str "content:" error-page-key "/title"))
           [:.clj--error-page-header] (html/set-attr :data-l8n (str "content:" error-page-key "/page-header"))
           [:.clj--error-page-intro] (html/set-attr :data-l8n (str "content:" error-page-key "/page-intro"))))

(defn internal-server-error [request]
  (vh/load-template-with-lang "public/error-500.html" (translation/get-locale-from-request request)))

(defn not-found-error [request]
  (-> (internal-server-error request)
      (modify-error-translation-keys "error-404")))

(defn forbidden-error [request]
  (-> (internal-server-error request)
      (modify-error-translation-keys "error-forbidden")))

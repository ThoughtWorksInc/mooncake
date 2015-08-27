(ns mooncake.view.error
  (:require [net.cgrand.enlive-html :as html]
            [mooncake.view.view-helpers :as vh]))

(defn modify-error-translation-keys [enlive-map error-page-key]
  (html/at enlive-map
           [:title] (html/set-attr :data-l8n (str "content:" error-page-key "/title"))
           [:body] (html/set-attr :class (str "func--" error-page-key "-page"))
           [:.clj--error-page-header] (html/set-attr :data-l8n (str "content:" error-page-key "/page-header"))
           [:.clj--error-page-intro] (html/set-attr :data-l8n (str "content:" error-page-key "/page-intro"))
           [:.clj--error-page-content] (html/set-attr :data-l8n (str "html:" error-page-key "/page-content"))))

(defn internal-server-error [request]
  (vh/load-template "public/error-500.html"))

(defn not-found-error [request]
  (-> (internal-server-error request) (modify-error-translation-keys "error-404")))

(defn forbidden-error [request]
  (-> (internal-server-error request) (modify-error-translation-keys "error-forbidden")))

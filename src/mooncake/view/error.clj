(ns mooncake.view.error
  (:require [net.cgrand.enlive-html :as html]
            [mooncake.view.view-helpers :as vh]))

(defn modify-error-translation-keys [enlive-map error-page-key]
  (html/at enlive-map
           [:title] (html/set-attr :data-l8n (str "content:" error-page-key "/title"))
           [:.clj--error-page-header] (html/set-attr :data-l8n (str "content:" error-page-key "/page-header"))
           [:.clj--error-page-intro] (html/set-attr :data-l8n (str "content:" error-page-key "/page-intro"))
           [:.clj--error-page-content] (html/set-attr :data-l8n (str "html:" error-page-key "/page-content"))))

(defn internal-server-error []
  (vh/load-template "public/error-500.html"))

(defn not-found-error []
  (-> (internal-server-error) (modify-error-translation-keys "error-404")))

(defn forbidden-error []
  (-> (internal-server-error) (modify-error-translation-keys "error-forbidden")))

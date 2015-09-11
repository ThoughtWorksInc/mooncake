(ns mooncake.view.customise-feed
  (:require [net.cgrand.enlive-html :as html]
            [mooncake.routes :as r]
            [mooncake.helper :as mh]
            [mooncake.view.view-helpers :as vh]))

(defn set-form-action [enlive-m]
  (html/at enlive-m [:.clj--customise-feed__form] (html/set-attr :action (r/path :customise-feed))))

(defn generate-feed-items [enlive-m activity-source-preferences]
  (let [feed-item-snippet (first (html/select enlive-m [:.clj--feed-item]))]
    (html/at feed-item-snippet [html/root]
             (html/clone-for [activity-source activity-source-preferences]
                             [:.clj--feed-item__label] (html/set-attr :for (:id activity-source))
                             [:.clj--feed-item__name] (html/content (:name activity-source))
                             [:.clj--feed-item__checkbox] (html/do->
                                                            (html/set-attr :name (:id activity-source))
                                                            (html/set-attr :id (:id activity-source))
                                                            (if (:selected activity-source)
                                                                (html/set-attr :checked "checked")
                                                                (html/remove-attr :checked)))))))

(defn add-feed-items [enlive-m activity-source-preferences]
  (html/at enlive-m
           [:.clj--customise-feed__list] (html/content (generate-feed-items enlive-m activity-source-preferences))))

(defn render-sign-out-link [enlive-m signed-in?]
  (if signed-in?
    (html/at enlive-m [:.clj--sign-out__link] (html/do->
                                                (html/remove-class "clj--STRIP")
                                                (html/set-attr :href (r/path :sign-out))))
    enlive-m))

(defn render-username [enlive-m username]
  (html/at enlive-m [:.clj--username] (html/content username)))

(defn customise-feed [request]
  (->
    (vh/load-template "public/customise-feed.html")
    (render-username (get-in request [:session :username]))
    (render-sign-out-link (mh/signed-in? request))
    (add-feed-items (get-in request [:context :activity-source-preferences]))
    set-form-action))


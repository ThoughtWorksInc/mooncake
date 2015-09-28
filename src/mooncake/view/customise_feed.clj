(ns mooncake.view.customise-feed
  (:require [net.cgrand.enlive-html :as html]
            [mooncake.routes :as r]
            [mooncake.helper :as mh]
            [mooncake.view.view-helpers :as vh]))

(defn set-form-action [enlive-m]
  (html/at enlive-m [:.clj--customise-feed__form] (html/set-attr :action (r/path :customise-feed))))

(defn create-activity-type-id [activity-source-id activity-type-name]
  (str activity-source-id "::" activity-type-name))

(defn- element-id->element-before-id [activity-element-id]
  (str (name activity-element-id) "::before"))

(defn generate-feed-item-children [enlive-m activity-source]
  (let [feed-item-child-snippet (first (html/select enlive-m [:.clj--feed-item-child]))]
    (html/at feed-item-child-snippet [html/root]
             (html/clone-for [activity-type (:activity-types activity-source)
                              :let [activity-type-id (create-activity-type-id (:id activity-source) (:id activity-type))]]
                             [:.clj--feed-item-child__label] (html/set-attr :for activity-type-id)
                             [:.clj--feed-item-child__name] (html/content (:id activity-type))
                             [:.clj--feed-item-child__checkbox] (html/do->
                                                                  (html/set-attr :name activity-type-id)
                                                                  (html/set-attr :id activity-type-id)
                                                                  (if (:selected activity-type)
                                                                    (html/set-attr :checked "checked")
                                                                    (html/remove-attr :checked)))
                             [:.clj--feed-item-child__input_hidden] (html/do->
                                                                     (html/set-attr :name (element-id->element-before-id activity-type-id))
                                                                     (html/set-attr :value (str (:selected activity-type))))))))

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
                                                              (html/remove-attr :checked)))
                             [:.clj--feed-item__input_hidden] (html/do->
                                                            (html/set-attr :name (element-id->element-before-id (:id activity-source)))
                                                            (html/set-attr :value (str (:selected activity-source))))
                             [:.clj--feed-item__children-list] (html/content (generate-feed-item-children enlive-m activity-source))))))

(defn add-feed-items [enlive-m activity-source-preferences]
  (html/at enlive-m
           [:.clj--customise-feed__list] (html/content (generate-feed-items enlive-m activity-source-preferences))))

(defn render-sign-out-link [enlive-m signed-in?]
  (if signed-in?
    (html/at enlive-m [:.clj--sign-out__link] (html/do->
                                                (html/remove-class "clj--STRIP")
                                                (html/set-attr :href (r/path :sign-out))))
    enlive-m))

(defn render-customise-feed-link [enlive-m signed-in?]
  (if signed-in?
    (html/at enlive-m [:.clj--customise-feed__link] (html/do->
                                                      (html/remove-class "clj--STRIP")
                                                      (html/set-attr :href (r/path :show-customise-feed))))
    enlive-m))

(defn render-username [enlive-m username]
  (html/at enlive-m [:.clj--username] (html/content username)))

(defn customise-feed [request]
  (->
    (vh/load-template "public/customise-feed.html")
    (render-username (get-in request [:session :username]))
    (render-sign-out-link (mh/signed-in? request))
    (render-customise-feed-link (mh/signed-in? request))
    (add-feed-items (get-in request [:context :activity-source-preferences]))
    set-form-action))


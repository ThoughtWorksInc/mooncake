(ns mooncake.view.index
  (:require [net.cgrand.enlive-html :as html]
            [mooncake.helper :as h]
            [mooncake.view.view-helpers :as vh]))


(defn generate-activity-stream-items [enlive-m activities]
  (let [activity-stream-item (html/select enlive-m [[:.clj--activity-item html/first-of-type]])]
    (html/at activity-stream-item [html/root]
             (html/clone-for [activity activities]
                             [:.clj--activity-item__link] (html/set-attr :href (get-in activity ["object" "url"]))
                             [:.clj--activity-item__time] (let [activity-time (get activity "published")]
                                                            (html/do->
                                                              (html/set-attr :datetime activity-time)
                                                              (html/content (when activity-time
                                                                              (h/humanise-time activity-time)))))
                             [:.clj--activity-item__action] (html/content (str (get-in activity ["actor" "displayName"]) " - "
                                                                               (get-in activity ["object" "@type"])      " - "
                                                                               (get activity "@type")))
                             [:.clj--activity-item__title] (html/content (get-in activity ["object" "displayName"]))))))

(defn add-activities [enlive-m activities]
  (let [activity-stream-items (generate-activity-stream-items enlive-m activities)]
    (html/at enlive-m [:.clj--activity-stream]
             (html/content activity-stream-items))))

(defn index [request]
  (let [activities (get-in request [:context :activities])]
    (->
      (vh/load-template "public/index.html")
      (add-activities activities))))

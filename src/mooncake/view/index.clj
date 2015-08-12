(ns mooncake.view.index
  (:require [net.cgrand.enlive-html :as html]
            [mooncake.routes :as routes]
            [mooncake.helper :as h]
            [mooncake.view.view-helpers :as vh]))

(defn index-activity-sources [activities]
  (zipmap (distinct (map :activity-src activities)) (range)))

(defn generate-activity-stream-items [enlive-m activities]
  (let [activity-source-indexes (index-activity-sources activities)
        activity-stream-item (html/select enlive-m [[:.clj--activity-item html/first-of-type]])]
    (html/at activity-stream-item [html/root]
             (html/clone-for [activity activities]
                             [:.clj--activity-item] (html/do->
                                                      (html/add-class (str "activity-src-"
                                                                           ((:activity-src activity) activity-source-indexes))))
                             [:.clj--avatar__initials] (html/content (-> (get-in activity ["actor" "displayName"])
                                                                         first str clojure.string/upper-case))
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

(defn set-sign-out-link [enlive-m]
  (html/at enlive-m [:.clj--sign-out__link] (html/set-attr :href (routes/path :sign-out))))

(defn index [request]
  (let [activities (get-in request [:context :activities])]
    (-> (vh/load-template "public/index.html")
        set-sign-out-link
        (add-activities activities))))

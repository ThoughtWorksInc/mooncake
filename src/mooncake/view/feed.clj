(ns mooncake.view.feed
  (:require [net.cgrand.enlive-html :as html]
            [mooncake.routes :as routes]
            [mooncake.helper :as mh]
            [mooncake.view.view-helpers :as vh]
            [mooncake.domain.activity :as domain]))

(defn index-activity-sources [activities]
  (zipmap (distinct (map domain/activity->activity-src activities)) (range)))

(defn generate-activity-stream-items [enlive-m activities]
  (let [activity-source-indexes (index-activity-sources activities)
        activity-stream-item (html/select enlive-m [[:.clj--activity-item html/first-of-type]])]
    (html/at activity-stream-item [html/root]
             (html/clone-for [activity activities]
                             [:.clj--activity-item] (html/do->
                                                      (html/add-class (str "activity-src-"
                                                                           (get activity-source-indexes (domain/activity->activity-src activity)))))
                             [:.clj--avatar__initials] (html/content (-> (domain/activity->actor-display-name activity)
                                                                         first str clojure.string/upper-case))
                             [:.clj--activity-item__link] (html/set-attr :href (domain/activity->object-url activity))
                             [:.clj--activity-item__time] (let [activity-time (domain/activity->published activity)]
                                                            (html/do->
                                                              (html/set-attr :datetime activity-time)
                                                              (html/content (when activity-time
                                                                              (mh/humanise-time activity-time)))))
                             [:.clj--activity-item__action] (html/content (str (domain/activity->actor-display-name activity) " - "
                                                                               (domain/activity->object-type activity) " - "
                                                                               (domain/activity->type activity)))
                             [:.clj--activity-item__title] (html/content (domain/activity->object-display-name activity))))))

(defn add-activities [enlive-m activities]
  (let [activity-stream-items (generate-activity-stream-items enlive-m activities)]
    (html/at enlive-m [:.clj--activity-stream]
             (html/content activity-stream-items))))

(defn render-sign-out-link [enlive-m signed-in?]
  (if signed-in?
    (html/at enlive-m [:.clj--sign-out__link] (html/do->
                                                (html/remove-class "clj--STRIP")
                                                (html/set-attr :href (routes/path :sign-out))))
    enlive-m))

(defn render-username [enlive-m username]
  (html/at enlive-m [:.clj--username] (html/content username)))

(defn feed [request]
  (let [activities (get-in request [:context :activities])]
    (-> (vh/load-template "public/feed.html")
        (render-username (get-in request [:session :username]))
        (render-sign-out-link (mh/signed-in? request))
        (add-activities activities))))

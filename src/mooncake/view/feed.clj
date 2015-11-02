(ns mooncake.view.feed
  (:require [net.cgrand.enlive-html :as html]
            [mooncake.routes :as routes]
            [mooncake.helper :as mh]
            [mooncake.view.view-helpers :as vh]
            [mooncake.domain.activity :as domain]))

(def max-characters-in-title 140)

(defn index-activity-sources [activities]
  (zipmap (distinct (map domain/activity->activity-src activities)) (range)))

(defn activity-action-message-translation [activity-action-key]
  (case activity-action-key
    :objective "feed/action-text-objective"
    :question "feed/action-text-question"
    nil))

(defn activity-action-message-map [text]
 (when text (str "content:" text)))

(defn generate-activity-stream-items [enlive-m activities]
  (let [activity-source-indexes (index-activity-sources activities)
        activity-stream-item (html/select enlive-m [[:.clj--activity-item html/first-of-type]])
        library-m (vh/load-template "public/library.html")
        activity-stream-item-untrusted-source-snippet (first (html/select library-m [:.clj--activity-item__suspicious--untrusted-source]))
        activity-stream-item-unverified-signature-snippet (first (html/select library-m [:.clj--activity-item__suspicious--unverified-signature]))
        original-activity-src-class (re-find #"activity-src-\d+" (-> activity-stream-item first :attrs :class))]
    (html/at activity-stream-item [html/root]
             (html/clone-for [activity activities]
                             [:.clj--activity-item] (html/do->
                                                      (html/remove-class original-activity-src-class)
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
                             [:.clj--activity-item__action__author] (html/content (domain/activity->actor-display-name activity))
                             [:.clj--activity-item__action] (let [action-text-key (domain/activity->action-text-key activity)]
                                                              (if (= :default action-text-key)
                                                                (html/content (domain/activity->default-action-text activity))
                                                                (html/set-attr :data-l8n (activity-action-message-map (activity-action-message-translation action-text-key)))))
                             [:.clj--activity-item__title] (html/content (vh/limit-text-length-if-above max-characters-in-title (domain/activity->object-display-name activity)))
                             [:.clj--activity-item__suspicious] (let [action-signed (domain/activity->signed activity)]
                                                                  (case action-signed
                                                                    (or false nil) (html/do->
                                                                                     (html/substitute activity-stream-item-untrusted-source-snippet)
                                                                                     (html/remove-class "clj--STRIP"))
                                                                    "verification-failed" (html/do->
                                                                                           (html/substitute activity-stream-item-unverified-signature-snippet)
                                                                                           (html/remove-class "clj--STRIP"))
                                                                    nil))))))

(defn add-activities [enlive-m activities]
  (let [activity-stream-items (generate-activity-stream-items enlive-m activities)]
    (html/at enlive-m [:.clj--activity-stream]
             (html/content activity-stream-items))))

(defn add-no-active-activity-sources-message [enlive-m]
  (html/at enlive-m
           [:.clj--activity-stream] (html/content (-> (vh/load-template "public/library.html")
                                                      (html/select [:.clj--empty-activity-item])
                                                      first))
           [:.clj--empty-stream__link] (html/set-attr :href (routes/path :show-customise-feed))))

(defn render-sign-out-link [enlive-m signed-in?]
  (if signed-in?
    (html/at enlive-m [:.clj--sign-out__link] (html/do->
                                                (html/remove-class "clj--STRIP")
                                                (html/set-attr :href (routes/path :sign-out))))
    enlive-m))

(defn render-customise-feed-link [enlive-m signed-in?]
  (if signed-in?
    (html/at enlive-m [:.clj--customise-feed__link] (html/do->
                                                      (html/remove-class "clj--STRIP")
                                                      (html/set-attr :href (routes/path :show-customise-feed))))
    enlive-m))

(defn render-username [enlive-m username]
  (html/at enlive-m [:.clj--username] (html/content username)))

(defn render-activity-stream [enlive-m activities]
  (if (empty? activities)
    (add-no-active-activity-sources-message enlive-m)
    (add-activities enlive-m activities)))

(defn feed-path-url-with-page-number [value]
  (str (routes/path :feed) "?page-number=" value))

(defn render-newer-activities-link [enlive-m page-number]
  (let [page-number-not-null (or page-number 1)
        dec-page-number (dec page-number-not-null)]
    (if (= page-number-not-null 1)
      (html/at enlive-m [:.clj--newer-activities__link] (html/do->
                                                          (html/add-class "clj--STRIP")))
      (html/at enlive-m [:.clj--newer-activities__link] (html/do->
                                                          (html/set-attr :href (feed-path-url-with-page-number dec-page-number)))))))

(defn render-older-activities-link [enlive-m is-last-page? page-number]
  (let [page-number-not-null (or page-number 1)
        inc-page-number (inc page-number-not-null)]
    (if is-last-page?
      (html/at enlive-m [:.clj--older-activities__link] (html/do->
                                                          (html/add-class "clj--STRIP")))
      (html/at enlive-m [:.clj--older-activities__link] (html/do->
                                                          (html/set-attr :href (feed-path-url-with-page-number inc-page-number)))))))
(defn feed [request]
  (let [activities (get-in request [:context :activities])]
    (-> (vh/load-template "public/feed.html")
        (render-username (get-in request [:session :username]))
        (render-customise-feed-link (mh/signed-in? request))
        (render-sign-out-link (mh/signed-in? request))
        (render-activity-stream activities)
        (render-older-activities-link (get-in request [:context :is-last-page])
                                      (get-in request [:params :page-number]))
        (render-newer-activities-link (get-in request [:params :page-number]))
        (vh/add-script "js/main.js"))))
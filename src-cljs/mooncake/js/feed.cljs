(ns mooncake.js.feed
  (:require [ajax.core :refer [GET]]
            [dommy.core :as d]
            [mooncake.js.dom :as dom])
  (:require-macros [dommy.core :as dm]
                   [mooncake.js.config :refer [polling-interval-ms]]))

(def reveal-new-activities-link :.clj--reveal-new-activities__link)

(defn set-author-initials! [activity feed-item]
  (let [author (get-in activity ["actor" "displayName"])
        initial (-> author first str clojure.string/upper-case)]
    (d/set-text! (dm/sel1 feed-item :.clj--avatar__initials) initial)))

(defn set-author! [activity feed-item]
  (let [author (get-in activity ["actor" "displayName"])]
    (d/set-text! (dm/sel1 feed-item :.clj--activity-item__action__author) author)))

(defn set-title! [activity feed-item]
  (let [title (get activity "limited-title")]
    (d/set-text! (dm/sel1 feed-item :.clj--activity-item__title) title)))

(defn set-time! [activity feed-item]
  (let [human-readable-time (get activity "formatted-time")
        time (get activity "published")]
    (d/set-attr! (dm/sel1 feed-item :.clj--activity-item__time) "datetime" time)
    (d/set-text! (dm/sel1 feed-item :.clj--activity-item__time) human-readable-time)))

(defn set-link! [activity feed-item]
  (let [link (get-in activity ["object" "url"])]
    (d/set-attr! (dm/sel1 feed-item :.clj--activity-item__link) "href" link)))

(defn set-src-class! [activity feed-item]
  (let [prev-activity-source-class (re-find #"activity-src-\d+" (d/class feed-item))
        activity-source-class (str "activity-src-" (get activity "activity-src-no"))]
    (d/remove-class! feed-item prev-activity-source-class)
    (d/add-class! feed-item activity-source-class)))

(defn set-action! [activity feed-item]
  (let [action-text (get activity "action-text")]
    (d/remove-attr! (dm/sel1 feed-item :.clj--activity-item__action) "data-l8n")
    (d/set-text! (dm/sel1 feed-item :.clj--activity-item__action) action-text)))

(defn- create-new-feed-item [activity new-feed-item]
  (set-author-initials! activity new-feed-item)
  (set-author! activity new-feed-item)
  (set-title! activity new-feed-item)
  (set-time! activity new-feed-item)
  (set-link! activity new-feed-item)
  (set-src-class! activity new-feed-item)
  (set-action! activity new-feed-item))

(def request-not-in-progress
  (atom true))

(defn append-old-activities [load-activities-fn response]
  (let [activity-stream (dm/sel1 :.clj--activity-stream)
        original-list-html (. activity-stream -innerHTML)]
    (set! (. activity-stream -innerHTML) (str original-list-html response))
    (if (empty? response)
      (do (reset! request-not-in-progress true)
          (d/unlisten! js/window :scroll load-activities-fn))
      (do (reset! request-not-in-progress true)
          (load-activities-fn)))))

(defn older-activities-handler [load-activities-fn response]
  (js/setTimeout
    #(append-old-activities load-activities-fn response)
    1000))

(defn new-activities-error-handler [response]
  (d/add-class! (dm/sel1 :.clj--new-activities__error) "show-feed-activities__error"))

(defn old-activities-error-handler [response]
  (.log js/console (str "something bad happened: " response)))

(defn load-old-activities [load-activities-fn]
  (let [stream (dm/sel1 :.clj--activity-stream)
        last-activity (.-lastChild stream)
        selector (dm/sel1 last-activity :.clj--activity-item__time)
        timestamp (d/attr selector "datetime")]
    (when @request-not-in-progress
      (do (reset! request-not-in-progress false)
          (GET (str "/api/activities-html?timestamp-to=" timestamp)
               {:handler       (partial older-activities-handler load-activities-fn)
                :error-handler old-activities-error-handler})))))

(defn load-more-activities-if-at-end-of-page []
  (let [scroll-top (dom/get-scroll-top)
        scroll-height (dom/get-scroll-height)
        window-height (.-innerHeight js/window)
        scrolled-to-bottom? (>= (+ scroll-top window-height) scroll-height)]
    (when (and scrolled-to-bottom? (dom/body-has-class? "cljs--feed-page"))
      (load-old-activities load-more-activities-if-at-end-of-page))))

(defn update-new-activities-link-text [length]
  (d/set-text! (dm/sel1 :.func--reveal-new-activities__link) (str "View " length " new activities")))

(defn reveal-new-activities [e]
  (let [new-activities (dm/sel :.hidden-new-activity)]
    (doseq [activity new-activities]
      (d/remove-class! activity "hidden-new-activity")))
  (d/remove-class! (.-target e) "show-new-activities__link"))

(defn newer-activities-handler [polling-fn response]
  (let [activities (get response "activities")
        feed-item (dm/sel1 :.clj--activity-item)]
    (doseq [activity (reverse activities)]
      (let [new-feed-item (.cloneNode feed-item true)]
        (create-new-feed-item activity new-feed-item)
        (d/add-class! new-feed-item "hidden-new-activity")
        (d/prepend! (dm/sel1 :.clj--activity-stream) new-feed-item)))
    (if (not (empty? activities))
      (let [show-new-items-link (dm/sel1 :.func--reveal-new-activities__link)]
              (update-new-activities-link-text (count activities))
              (dom/add-if-not-present show-new-items-link "show-new-activities__link"))))
  (polling-fn))

(defn hide-pagination-buttons []
  (dom/remove-if-present! :.clj--newer-activities__link)
  (dom/remove-if-present! :.clj--older-activities__link))

(defn load-new-activities [polling-fn]
  (let [stream (dm/sel1 :.clj--activity-stream)
        last-activity (.-firstChild stream)
        selector (dm/sel1 last-activity :.clj--activity-item__time)
        timestamp (d/attr selector "datetime")]
    (GET (str "/api/activities?timestamp-from=" timestamp)
         {:handler  (partial newer-activities-handler polling-fn)
          :error-handler new-activities-error-handler})))

(defn check-for-new-activities []
  (js/setTimeout
    #(load-new-activities check-for-new-activities)
    (polling-interval-ms)))
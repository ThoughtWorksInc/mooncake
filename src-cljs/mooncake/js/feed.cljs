(ns mooncake.js.feed
  (:require [ajax.core :refer [GET]]
            [dommy.core :as d]
            [mooncake.js.dom :as dom])
  (:require-macros [dommy.core :as dm]
                   [mooncake.js.config :refer [polling-interval-ms]]))

(def reveal-new-activities-link :.clj--reveal-new-activities__link)

(defn hide-pagination-buttons []
  (dom/remove-if-present! :.clj--newer-activities__link)
  (dom/remove-if-present! :.clj--older-activities__link))

(def request-not-in-progress
  (atom true))

(defn old-activities-error-handler [response]
  (.log js/console (str "something bad happened: " response)))

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

(defn reveal-new-activities [e]
  (let [new-activities (dm/sel :.hidden-new-activity)]
    (doseq [activity new-activities]
      (d/remove-class! activity "hidden-new-activity")))
  (d/remove-class! (.-target e) "show-new-activities__link"))

(defn new-activities-error-handler [response]
  (d/add-class! (dm/sel1 :.clj--new-activities__error) "show-feed-activities__error"))

(defn update-new-activities-link-text [length]
  (d/set-text! (dm/sel1 :.func--reveal-new-activities__link) (str "View " length " new activities")))

(defn newer-activities-handler [polling-fn response]
  (let [activity-stream (dm/sel1 :.clj--activity-stream)
        original-list-html (. activity-stream -innerHTML)]
    (when (not (empty? response))
      (set! (. activity-stream -innerHTML) (str response original-list-html))
      (let [show-new-items-link (dm/sel1 :.func--reveal-new-activities__link)
            new-activities-count (count (re-seq #"<li" response))]
        (update-new-activities-link-text new-activities-count)
        (dom/add-if-not-present show-new-items-link "show-new-activities__link")))
  (polling-fn)))

(defn load-new-activities [polling-fn]
  (let [stream (dm/sel1 :.clj--activity-stream)
        last-activity (.-firstChild stream)
        selector (dm/sel1 last-activity :.clj--activity-item__time)
        timestamp (d/attr selector "datetime")]
    (GET (str "/api/activities-html?timestamp-from=" timestamp)
         {:handler  (partial newer-activities-handler polling-fn)
          :error-handler new-activities-error-handler})))

(defn check-for-new-activities []
  (js/setTimeout
    #(load-new-activities check-for-new-activities)
    (polling-interval-ms)))
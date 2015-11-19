(ns mooncake.js.feed
  (:require [ajax.core :refer [GET]]
            [dommy.core :as d]
            [mooncake.js.dom :as dom]
            [hickory.core :as hic])
  (:require-macros [dommy.core :as dm]
                   [mooncake.js.config :refer [polling-interval-ms]]))

(def reveal-new-activities-link :.clj--reveal-new-activities__link)
(def activity-loading-spinner :.clj--activity-loading-spinner)

(defn hide-pagination-buttons []
  (dom/remove-if-present! :.clj--newer-activities__link)
  (dom/remove-if-present! :.clj--older-activities__link))

(def request-not-in-progress
  (atom true))

(def number-of-hidden-activities
  (atom 0))

(defn older-activities-error-handler [response]
  (.log js/console (str "something bad happened: " response)))

(defn valid-response? [response]
  (let [hickory-response (map hic/as-hickory (hic/parse-fragment response))
        valid-tag-seq (map #(= (:tag %) :li) hickory-response)]
    (every? true? valid-tag-seq)))

(defn append-older-activities [load-activities-fn response]
  (let [activity-stream (dm/sel1 :.clj--activity-stream)
        original-list-html (. activity-stream -innerHTML)]
    (d/set-attr! (dm/sel1 activity-loading-spinner) "hidden")
    (set! (. activity-stream -innerHTML) (str original-list-html response))
    (reset! request-not-in-progress true)
    (load-activities-fn)))

(defn older-activities-handler [load-activities-fn response]
  (if (or (empty? response) (not (valid-response? response)))
    (do (reset! request-not-in-progress true)
        (d/unlisten! js/window :scroll load-activities-fn)
        (d/set-attr! (dm/sel1 activity-loading-spinner) "hidden"))
    (js/setTimeout
      #(append-older-activities load-activities-fn response)
      1000)))

(defn load-older-activities [load-activities-fn]
  (when @request-not-in-progress
    (reset! request-not-in-progress false)
    (let [timestamp (d/attr (last (dm/sel :.clj--activity-item__time)) "datetime")
          id (d/text (last (dm/sel :.clj--activity-item__id)))]
      (GET (str "/api/activities-html?timestamp-to=" timestamp "&insert-id=" id)
           {:handler       (partial older-activities-handler load-activities-fn)
            :error-handler older-activities-error-handler})
      (d/remove-attr! (dm/sel1 activity-loading-spinner) "hidden"))))

(defn load-more-activities-if-at-end-of-page []
  (let [scroll-top (dom/get-scroll-top)
        scroll-height (dom/get-scroll-height)
        window-height (.-innerHeight js/window)
        scrolled-to-bottom? (>= (+ scroll-top window-height) scroll-height)]
    (when (and scrolled-to-bottom? (dom/body-has-class? "cljs--feed-page"))
      (load-older-activities load-more-activities-if-at-end-of-page))))

(defn reveal-newer-activities [e]
  (let [new-activities (dm/sel :.hidden-new-activity)]
    (doseq [activity new-activities]
      (d/remove-class! activity "hidden-new-activity")))
  (d/remove-class! (.-target e) "show-new-activities__link")
  (reset! number-of-hidden-activities 0))

(defn newer-activities-error-handler [response]
  (d/add-class! (dm/sel1 :.clj--new-activities__error) "show-feed-activities__error"))

(defn get-translation [key]
  (get-in dom/translations [:feed key]))

(defn update-new-activities-link-text [length]
  (let [message-start-key (if (> length 1) :new-activities-message-start :new-activity-message-start)
        message-end-key (if (> length 1) :new-activities-message-end :new-activity-message-end)
        message-start-translation (get-translation message-start-key)
        message-end-translation (get-translation message-end-key)
        message (str message-start-translation length message-end-translation)]
    (d/set-text! (dm/sel1 :.func--reveal-new-activities__link) message)))

(defn newer-activities-handler [polling-fn response]
  (let [activity-stream (dm/sel1 :.clj--activity-stream)
        original-list-html (. activity-stream -innerHTML)]
    (when (valid-response? response)
      (when (not (empty? response))
        (set! (. activity-stream -innerHTML) (str response original-list-html))
        (let [show-new-items-link (dm/sel1 :.func--reveal-new-activities__link)
              new-activities-count (+ @number-of-hidden-activities (count (re-seq #"<li" response)))]
          (reset! number-of-hidden-activities new-activities-count)
          (update-new-activities-link-text new-activities-count)
          (dom/add-if-not-present show-new-items-link "show-new-activities__link")))
      (polling-fn))))

(defn load-new-activities [polling-fn]
  (let [timestamp (d/attr (first (dm/sel :.clj--activity-item__time)) "datetime")
        id (d/text (first (dm/sel :.clj--activity-item__id)))]
    (GET (str "/api/activities-html?timestamp-from=" timestamp "&insert-id=" id)
         {:handler       (partial newer-activities-handler polling-fn)
          :error-handler newer-activities-error-handler})))

(defn check-for-new-activities []
  (js/setTimeout
    #(load-new-activities check-for-new-activities)
    (polling-interval-ms)))
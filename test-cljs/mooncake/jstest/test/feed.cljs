(ns mooncake.jstest.test.feed
  (:require [cemerick.cljs.test]
            [mooncake.js.feed :as feed]
            [dommy.core :as d]
            [mooncake.jstest.test-utils :as tu]
            [mooncake.js.feed :as feed]
            [dommy.core :as dommy])
  (:require-macros [cemerick.cljs.test :refer [deftest is testing]]
                   [mooncake.jstest.macros :refer [load-template]]
                   [dommy.core :as dm]))

(defonce feed-page-template (load-template "public/feed.html"))

(defn set-initial-state []
  (tu/set-html! feed-page-template))

(deftest removes-event-listener
         (testing "scroll listener is removed if response contains no activities"
                  (set-initial-state)
                  (feed/append-old-activities feed/load-more-activities-if-at-end-of-page "")
                  (is (empty? (dommy/event-listeners js/window)))))

(deftest about-hiding-pagination-buttons
         (testing "pagination buttons are hidden on page load"
                  (set-initial-state)
                  (feed/hide-pagination-buttons)
                  (is (nil? (dm/sel1 :.func--older-activities__link))
                      "older button does not exist")
                  (is (nil? (dm/sel1 :.func--newer-activities__link))
                      "new button does not exist")))

(deftest updates-new-activity-link-text
         (testing "activity link text is set to correct string"
                  (set-initial-state)
                  (let [new-activity-link (feed/update-new-activities-link-text 5)]
                    (tu/test-string-contains (d/text new-activity-link) "5"))))

(deftest about-displaying-new-activities-error
         (testing "error handler displays error message"
                  (set-initial-state)
                  (tu/test-string-does-not-contain (d/class (dm/sel1 :.clj--new-activities__error)) "show-feed-activities__error")
                  (feed/new-activities-error-handler {})
                  (tu/test-string-contains (d/class (dm/sel1 :.clj--new-activities__error)) "show-feed-activities__error")))
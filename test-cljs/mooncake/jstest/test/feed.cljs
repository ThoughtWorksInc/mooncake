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

(def activity {"actor"           {"displayName" "Bob"}
               "limited-title"   "Bob's Activity"
               "formatted-time"  "2 weeks ago"
               "published"       "2016-12-11T01:24:45.192Z"
               "object"          {"url" "http://activity-src.co.uk/bob"}
               "activity-src-no" "1"
               "action-text"     "created an activity"})

(defn create-feed-item []
  (let [feed-item-html
        "<li class=\"activity-src-0 clj--activity-item\">
          <a href class=\"clj--activity-item__link\">
            <span class=\"clj--avatar__initials\"></span>
            <span class=\"clj--activity-item__action__author\"></span>
            <span class=\"clj--activity-item__action\" data-l8n=\"content:feed/action-text-question\"></span>
            <time datetime class=\"clj--activity-item__time\"></time>
            <h3 class=\"clj--activity-item__title\"></h3>
          </a>
        </li>"
        feed-item (d/create-element "li")]
    (set! (. feed-item -innerHTML) feed-item-html)
    feed-item))

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

(deftest adds-spinner-before-adding-old-activities
         (testing "spinner appears on load-old-activities"
                  (set-initial-state)
                  (is (not (nil? (d/attr (dm/sel1 :.clj--activity-loading-spinner) "hidden"))))
                  (feed/load-old-activities (constantly {}))
                  (is (= (d/attr (dm/sel1 :.clj--activity-loading-spinner) "hidden") nil))))

(deftest removes-spinner-after-adding-old-activities
         (testing "spinner gets removed when activities are appended"
                  (set-initial-state)
                  (feed/load-old-activities (constantly {}))
                  (feed/append-old-activities {} "")
                  (is (not (nil? (d/attr (dm/sel1 :.clj--activity-loading-spinner) "hidden"))))))
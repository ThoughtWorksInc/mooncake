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

(def activity {"actor" {"displayName" "Bob"}
               "limited-title" "Bob's Activity"
               "formatted-time" "2 weeks ago"
               "published" "2016-12-11T01:24:45.192Z"
               "object" {"url" "http://activity-src.co.uk/bob"}
               "activity-src-no" "1"
               "action-text" "created an activity"})

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

(deftest updates-activity-information
         (testing "author is updated"
                  (let [author-elem (feed/set-author! activity (create-feed-item))]
                    (is (= (d/text author-elem) "Bob"))))
         (testing "id is updated"
                  (let [id-elem (feed/set-author-initials! activity (create-feed-item))]
                    (is (= (d/text id-elem) "B"))))
         (testing "title is updated"
                  (let [title-elem (feed/set-title! activity (create-feed-item))]
                    (is (= (d/text title-elem) "Bob's Activity"))))
         (testing "time is updated"
                  (let [time-elem (feed/set-time! activity (create-feed-item))]
                    (is (= (d/text time-elem) "2 weeks ago"))
                    (is (= (d/attr time-elem "datetime") "2016-12-11T01:24:45.192Z"))))
         (testing "link is updated"
                  (let [feed-item (create-feed-item)
                        link-elem (dm/sel1 feed-item :.clj--activity-item__link)]
                    (feed/set-link! activity feed-item)
                    (is (= (d/attr link-elem "href") "http://activity-src.co.uk/bob"))))
         (testing "source class is updated"
                  (let [src-class-elem (feed/set-src-class! activity (create-feed-item))]
                    (tu/test-string-contains (d/class src-class-elem) "activity-src-1")))
         (testing "action is updated"
                  (let [action-elem (feed/set-action! activity (create-feed-item))]
                    (is (= (d/text action-elem) "created an activity"))
                    (is (= (d/attr action-elem "data-l8n") nil)))))

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
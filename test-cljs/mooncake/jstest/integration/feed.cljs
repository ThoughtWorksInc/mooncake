(ns mooncake.jstest.integration.feed
  (:require [cemerick.cljs.test]
            [mooncake.jstest.test-utils :as tu]
            [mooncake.js.app :as app]
            [mooncake.js.feed :as feed])
  (:require-macros [cemerick.cljs.test :refer [deftest is testing]]
                   [mooncake.jstest.macros :refer [load-template]]
                   [dommy.core :refer [sel1 sel]]))

(defonce feed-page-template (load-template "public/feed.html"))

(def response {"activities" [{"actor" {"displayName" "Bob"}
                             "limited-title" "Bob's Activity"
                             "formatted-time" "2 weeks ago"
                             "published" "2016-12-11T01:24:45.192Z"
                             "object" {"url" "http://activity-src.co.uk/bob"}
                             "activity-src-no" "1"
                             "action-text" "created an activity"}
                             {"actor" {"displayName" "Bob"}
                              "limited-title" "Bob's Activity"
                              "formatted-time" "2 weeks ago"
                              "published" "2016-12-11T01:24:45.192Z"
                              "object" {"url" "http://activity-src.co.uk/bob"}
                              "activity-src-no" "1"
                              "action-text" "created an activity"}]})

(defn set-initial-state []
  (tu/set-html! feed-page-template)
  (app/start))

(deftest about-hiding-pagination-buttons
         (testing "pagination buttons are hidden on page load"
                  (set-initial-state)
                  (is (nil? (sel1 :.func--older-activities__link))
                      "older button does not exist")
                  (is (nil? (sel1 :.func--newer-activities__link))
                      "new button does not exist")))

(deftest about-loading-more-activities
         (testing "load more button converts json into new activity elements"
                  (set-initial-state)
                  (is (= 14 (count (sel :.clj--activity-item))))
                  (feed/handler response)
                  (is (= 16 (count (sel :.clj--activity-item))))))
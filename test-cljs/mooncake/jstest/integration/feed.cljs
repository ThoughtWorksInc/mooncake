(ns mooncake.jstest.integration.feed
  (:require [cemerick.cljs.test]
            [mooncake.jstest.test-utils :as tu]
            [mooncake.js.app :as app]
            [mooncake.js.feed :as feed]
            [dommy.core :as dommy])
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
                             {"actor" {"displayName" "Bill"}
                              "limited-title" "Bill's Activity"
                              "formatted-time" "2 weeks ago"
                              "published" "2016-12-11T01:24:45.192Z"
                              "object" {"url" "http://activity-src.co.uk/bill"}
                              "activity-src-no" "1"
                              "action-text" "created an activity"}]})

(defn set-initial-state []
  (tu/set-html! feed-page-template))

(deftest about-loading-more-activities
         (testing "load more button converts json into new activity elements"
                  (set-initial-state)
                  (is (= 14 (count (sel :.clj--activity-item))))
                  (feed/append-new-activities (constantly nil) response)
                  (let [activity-items (sel :.clj--activity-item)
                        activity-15 (nth activity-items 14)
                        activity-16 (nth activity-items 15)]
                    (is (= 16 (count activity-items)))
                    (is (= (dommy/text (sel1 activity-15 :.clj--activity-item__title)) "Bob's Activity"))
                    (is (= (dommy/text (sel1 activity-16 :.clj--activity-item__title)) "Bill's Activity")))))
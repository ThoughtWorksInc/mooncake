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
                             "published" "2016-12-12T01:24:45.192Z"
                             "object" {"url" "http://activity-src.co.uk/bob"}
                             "activity-src-no" "1"
                             "action-text" "created an activity"}
                             {"actor" {"displayName" "Bill"}
                              "limited-title" "Bill's Activity"
                              "formatted-time" "2 weeks ago"
                              "published" "2016-12-12T01:24:45.192Z"
                              "object" {"url" "http://activity-src.co.uk/bill"}
                              "activity-src-no" "1"
                              "action-text" "created an activity"}]})

(def empty-response {"activities" []})

(defn set-initial-state []
  (tu/set-html! feed-page-template))

(deftest about-loading-old-activities
         (testing "load more button converts json into new activity elements"
                  (set-initial-state)
                  (is (= 14 (count (sel :.clj--activity-item))))
                  (feed/append-old-activities (constantly nil) response)
                  (let [activity-items (sel :.clj--activity-item)
                        activity-15 (nth activity-items 14)
                        activity-16 (nth activity-items 15)]
                    (is (= 16 (count activity-items)))
                    (is (= (dommy/text (sel1 activity-15 :.clj--activity-item__title)) "Bob's Activity"))
                    (is (= (dommy/text (sel1 activity-16 :.clj--activity-item__title)) "Bill's Activity")))))

(deftest about-loading-new-activities
          (testing "activities are prepended in the correct order"
                   (set-initial-state)
                   (is (= 14 (count (sel :.clj--activity-item))))
                   (feed/newer-activities-handler empty-response)
                   (let [activity-items (sel :.clj--activity-item)]
                     (is (= 14 (count activity-items))))
                   (feed/newer-activities-handler response)
                   (let [activity-items (sel :.clj--activity-item)
                         activity-1 (nth activity-items 0)
                         activity-2 (nth activity-items 1)]
                     (is (= 16 (count activity-items)))
                     (is (= (dommy/text (sel1 activity-1 :.clj--activity-item__title)) "Bob's Activity"))
                     (is (= (dommy/text (sel1 activity-2 :.clj--activity-item__title)) "Bill's Activity"))))
         (testing "new activities triggers load new activities link"
                  (set-initial-state)
                  (feed/newer-activities-handler empty-response)
                  (tu/test-string-does-not-contain (dommy/class (sel1 :.func--reveal-new-activities__link)) "show-new-activities-link")
                  (feed/newer-activities-handler response)
                  (tu/test-string-contains (dommy/class (sel1 :.func--reveal-new-activities__link)) "show-new-activities-link")))


(ns mooncake.jstest.integration.feed
  (:require [cemerick.cljs.test]
            [dommy.core :as dommy]
            [mooncake.jstest.test-utils :as tu]
            [mooncake.js.app :as app]
            [mooncake.js.feed :as feed])
  (:require-macros [cemerick.cljs.test :refer [deftest is testing]]
                   [mooncake.jstest.macros :refer [load-template generate-test-html-data type-key]]
                   [dommy.core :refer [sel1 sel]]))

(defonce feed-page-template (load-template "public/feed.html"))

(def response {"activities" [{"actor" {"displayName" "Bob"}
                              "limited-title" "Bob's Activity"
                              "formatted-time" "2 weeks ago"
                              "published" "2016-12-12T01:24:45.192Z"
                              "object" {"url" "http://activity-src.co.uk/bob"}
                              "activity-src-no" "1"
                              "action-text" "created an activity"}
                             {"actor" {"displayName" "Jessie"}
                              "limited-title" "Jessie's Activity"
                              "formatted-time" "2 weeks ago"
                              "published" "2016-12-12T01:24:45.192Z"
                              "object" {"url" "http://activity-src.co.uk/Jessie"}
                              "activity-src-no" "1"
                              "action-text" "created an activity"}]})


(defn html-response []
  (generate-test-html-data {:activities [{:actor                            {:displayName "Bob"}
                                          :limited-title                    "Bob's Activity"
                                          :published                        "2012-12-12T01:24:45.192Z"
                                          (mooncake.jstest.macros/type-key) "Question"
                                          :object                           {:url                              "http://activity-src.co.uk/bob"
                                                                             :displayName                      "Save more trees?"
                                                                             (mooncake.jstest.macros/type-key) "Something"}
                                          :signed                           false}
                                         {:actor                            {:displayName "Margaret"}
                                          :limited-title                    "Margaret's Activity"
                                          :published                        "2012-12-12T01:24:45.192Z"
                                          (mooncake.jstest.macros/type-key) "Shut Down"
                                          :object                           {:url                              "http://activity-src.co.uk/margaret"
                                                                             :displayName                      "Save fewer mines!"
                                                                             (mooncake.jstest.macros/type-key) "Coal Mine"}
                                          :signed                           "verification-failed"}]}))

(def empty-response {"activities" []})

(defn set-initial-state []
  (tu/set-html! feed-page-template)
  (app/start))

(deftest about-loading-old-activities
         (testing "load more button converts json into new activity elements"
                  (set-initial-state)
                  (is (= 14 (count (sel :.clj--activity-item))))
                  (feed/append-old-activities (constantly nil) (html-response))
                  (let [activity-items (sel :.clj--activity-item)
                        activity-15 (nth activity-items 14)
                        activity-16 (nth activity-items 15)]
                    (is (= 16 (count activity-items)))
                    (is (= (dommy/text (sel1 activity-15 :.clj--avatar__initials)) "B"))
                    (is (= (dommy/attr (sel1 activity-15 :.clj--activity-item__link) "href") "http://activity-src.co.uk/bob"))
                    (is (not (empty? (dommy/text (sel1 activity-15 :.clj--activity-item__time)))))
                    (is (= (dommy/text (sel1 activity-15 :.clj--activity-item__action__author)) "Bob"))
                    (is (= (dommy/attr (sel1 activity-15 :.clj--activity-item__action) "data-l8n") "content:feed/action-text-question"))
                    (is (= (dommy/text (sel1 activity-15 :.clj--activity-item__title)) "Save more trees?"))
                    (is (not (nil? (sel1 activity-15 :.clj--activity-item__suspicious--untrusted-source))))

                    (is (= (dommy/text (sel1 activity-16 :.clj--avatar__initials)) "M"))
                    (is (= (dommy/attr (sel1 activity-16 :.clj--activity-item__link) "href") "http://activity-src.co.uk/margaret"))
                    (is (not (empty? (dommy/text (sel1 activity-16 :.clj--activity-item__time)))))
                    (is (= (dommy/text (sel1 activity-16 :.clj--activity-item__action__author)) "Margaret"))
                    (is (= (dommy/text (sel1 activity-16 :.clj--activity-item__action)) "- Coal Mine - Shut Down"))
                    (is (= (dommy/text (sel1 activity-16 :.clj--activity-item__title)) "Save fewer mines!"))
                    (is (not (nil? (sel1 activity-16 :.clj--activity-item__suspicious--unverified-signature)))))))

(deftest about-loading-new-activities
           (testing "activities are prepended in the correct order"
                    (set-initial-state)
                    (is (= 14 (count (sel :.clj--activity-item))))
                    (feed/newer-activities-handler (constantly nil) empty-response)
                    (let [activity-items (sel :.clj--activity-item)]
                      (is (= 14 (count activity-items))))
                    (feed/newer-activities-handler (constantly nil) response)
                    (let [activity-items (sel :.clj--activity-item)
                          activity-1 (nth activity-items 0)
                          activity-2 (nth activity-items 1)]
                      (is (= 16 (count activity-items)))
                      (is (= (dommy/text (sel1 activity-1 :.clj--activity-item__title)) "Bob's Activity"))
                      (is (= (dommy/text (sel1 activity-2 :.clj--activity-item__title)) "Jessie's Activity"))))
           (testing "new activities triggers load new activities link"
                    (set-initial-state)
                    (feed/newer-activities-handler (constantly nil) empty-response) (tu/test-string-does-not-contain (dommy/class (sel1 :.func--reveal-new-activities__link)) "show-new-activities__link")
                    (feed/newer-activities-handler (constantly nil) response)
                    (tu/test-string-contains (dommy/class (sel1 :.func--reveal-new-activities__link)) "show-new-activities__link"))
           (testing "number of new activities is displayed in new activities link"
                    (set-initial-state)
                    (feed/newer-activities-handler (constantly nil) response)
                    (tu/test-string-contains (dommy/text (sel1 :.func--reveal-new-activities__link)) "2"))
           (testing "new activities are hidden by default and revealed by clicking show activity link"
                    (set-initial-state)
                    (feed/newer-activities-handler (constantly nil) response)
                    (let [activity-items (sel :.clj--activity-item)
                          hidden-item-1 (nth activity-items 0)
                          hidden-item-2 (nth activity-items 1)]
                      (tu/test-string-contains (dommy/class hidden-item-1) "hidden-new-activity")
                      (tu/test-string-contains (dommy/class hidden-item-2) "hidden-new-activity")

                      (tu/click! :.func--reveal-new-activities__link)

                      (tu/test-string-does-not-contain (dommy/class hidden-item-1) "hidden-new-activity")
                      (tu/test-string-does-not-contain (dommy/class hidden-item-2) "hidden-new-activity")))
           (testing "reveal activities link is hidden after it is clicked"
                    (set-initial-state)
                    (feed/newer-activities-handler (constantly nil) response)
                    (tu/click! :.func--reveal-new-activities__link)
                    (tu/test-string-does-not-contain (dommy/class (sel1 :.func--reveal-new-activities__link)) "show-new-activities__link")))
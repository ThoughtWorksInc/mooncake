(ns mooncake.jstest.integration.feed
  (:require [cemerick.cljs.test]
            [dommy.core :as dommy]
            [mooncake.jstest.test-utils :as tu]
            [mooncake.js.app :as app]
            [mooncake.js.feed :as feed])
  (:require-macros [cemerick.cljs.test :refer [deftest is testing]]
                   [mooncake.jstest.macros :refer [load-template generate-test-html-data generate-test-html-data-hidden]]
                   [dommy.core :refer [sel1 sel]]
                   [mooncake.view.view-helpers :refer [update-language]]))

(defonce feed-page-template (load-template "public/feed.html"))

(defn html-response []
  (generate-test-html-data [{:actor     {:displayName "Bob"}
                             :published "2012-12-12T01:24:45.192Z"
                             :type      "Question"
                             :object    {:url         "http://activity-src.co.uk/bob"
                                         :displayName "Save more trees?"
                                         :type        "Something"}
                             :signed    false}
                            {:actor     {:displayName "Margaret"}
                             :published "2012-12-12T01:24:45.192Z"
                             :type      "Shut Down"
                             :object    {:url         "http://activity-src.co.uk/margaret"
                                         :displayName "Save fewer mines!"
                                         :type        "Coal Mine"}
                             :signed    "verification-failed"}]))

(defn html-response-hidden []
  (generate-test-html-data-hidden [{:actor     {:displayName "Bob"}
                                    :published "2012-12-12T01:24:45.192Z"
                                    :type      "Question"
                                    :object    {:url         "http://activity-src.co.uk/bob"
                                                :displayName "Save more trees?"
                                                :type        "Something"}
                                    :signed    false}
                                   {:actor     {:displayName "Margaret"}
                                    :published "2012-12-12T01:24:45.192Z"
                                    :type      "Shut Down"
                                    :object    {:url         "http://activity-src.co.uk/margaret"
                                                :displayName "Save fewer mines!"
                                                :type        "Coal Mine"}
                                    :signed    "verification-failed"}]))

(defn html-single-activity-response-hidden []
  (generate-test-html-data-hidden [{:actor     {:displayName "Bob"}
                                    :published "2012-12-12T01:24:45.192Z"
                                    :type      "Question"
                                    :object    {:url         "http://activity-src.co.uk/bob"
                                                :displayName "Save more trees?"
                                                :type        "Something"}
                                    :signed    false}]))

(def invalid-html-response "<a></a><a></a>")

(defn set-initial-state []
  (reset! feed/number-of-hidden-activities 0)
  (tu/set-html! feed-page-template)
  (tu/set-lang! "en")
  (app/start))

(deftest about-loading-old-activities
         (testing "load more button converts html into new activity elements"
                  (set-initial-state)
                  (is (= 14 (count (sel :.clj--activity-item))))
                  (feed/append-older-activities (constantly nil) (html-response))
                  (let [activity-items (sel :.clj--activity-item)
                        activity-15 (nth activity-items 14)
                        activity-16 (nth activity-items 15)]
                    (is (= 16 (count activity-items)))
                    (is (= (dommy/text (sel1 activity-15 :.clj--avatar__initials)) "B"))
                    (is (= (dommy/attr (sel1 activity-15 :.clj--activity-item__link) "href") "http://activity-src.co.uk/bob"))
                    (is (= (dommy/text (sel1 activity-15 :.clj--activity-item__time)) "3 years ago"))
                    (is (= (dommy/text (sel1 activity-15 :.clj--activity-item__action__author)) "Bob"))
                    (is (= (dommy/attr (sel1 activity-15 :.clj--activity-item__action) "data-l8n") "content:activity-type/action-text-something"))
                    (is (= (dommy/text (sel1 activity-15 :.clj--activity-item__title)) "Save more trees?"))
                    (is (not (nil? (sel1 activity-15 :.clj--activity-item__suspicious--untrusted-source))))

                    (is (= (dommy/text (sel1 activity-16 :.clj--avatar__initials)) "M"))
                    (is (= (dommy/attr (sel1 activity-16 :.clj--activity-item__link) "href") "http://activity-src.co.uk/margaret"))
                    (is (= (dommy/text (sel1 activity-16 :.clj--activity-item__time)) "3 years ago"))
                    (is (= (dommy/text (sel1 activity-16 :.clj--activity-item__action__author)) "Margaret"))
                    (is (= (dommy/text (sel1 activity-16 :.clj--activity-item__action)) "- Coal Mine - Shut Down"))
                    (is (= (dommy/text (sel1 activity-16 :.clj--activity-item__title)) "Save fewer mines!"))
                    (is (not (nil? (sel1 activity-16 :.clj--activity-item__suspicious--unverified-signature)))))))

(deftest about-loading-new-activities
         (testing "activities are prepended in the correct order"
                  (set-initial-state)
                  (is (= 14 (count (sel :.clj--activity-item))))
                  (feed/newer-activities-handler (constantly nil) "")
                  (let [activity-items (sel :.clj--activity-item)]
                    (is (= 14 (count activity-items))))
                  (feed/newer-activities-handler (constantly nil) (html-response-hidden))
                  (let [activity-items (sel :.clj--activity-item)
                        activity-1 (nth activity-items 0)
                        activity-2 (nth activity-items 1)]
                    (is (= 16 (count activity-items)))
                    (is (= (dommy/text (sel1 activity-1 :.clj--activity-item__title)) "Save more trees?"))
                    (is (= (dommy/text (sel1 activity-2 :.clj--activity-item__title)) "Save fewer mines!"))))
         (testing "new activities triggers load new activities link"
                  (set-initial-state)
                  (feed/newer-activities-handler (constantly nil) "")
                  (tu/test-string-does-not-contain (dommy/class (sel1 :.func--reveal-new-activities__link)) "show-new-activities__link")
                  (feed/newer-activities-handler (constantly nil) (html-response-hidden))
                  (tu/test-string-contains (dommy/class (sel1 :.func--reveal-new-activities__link)) "show-new-activities__link"))
         (testing "new activities link displays in correct language"
                  (set-initial-state)
                  (tu/set-lang! "fi")
                  (feed/newer-activities-handler (constantly nil) "")
                  (tu/test-string-does-not-contain (dommy/class (sel1 :.func--reveal-new-activities__link)) "show-new-activities__link")
                  (feed/newer-activities-handler (constantly nil) (html-response-hidden))
                  (tu/test-string-contains (dommy/text (sel1 :.func--reveal-new-activities__link)) "Finnish"))
         (testing "number of new activities is displayed in new activities link"
                  (let [new-activity-link-text (str (feed/t :en :new-activity-message-start) 1
                                                    (feed/t :en :new-activity-message-end))
                        new-activities-link-text (str (feed/t :en :new-activities-message-start) 3
                                                      (feed/t :en :new-activities-message-end))]
                    (set-initial-state)
                    (feed/newer-activities-handler (constantly nil) (html-single-activity-response-hidden))
                    (is (= (dommy/text (sel1 :.func--reveal-new-activities__link)) new-activity-link-text))
                    (feed/newer-activities-handler (constantly nil) (html-response-hidden))
                    (is (= (dommy/text (sel1 :.func--reveal-new-activities__link)) new-activities-link-text))))
         (testing "new activities are hidden by default and revealed by clicking show activity link"
                  (set-initial-state)
                  (feed/newer-activities-handler (constantly nil) (html-response-hidden))
                  (let [activity-items (sel :.clj--activity-item)
                        hidden-item-1 (nth activity-items 0)
                        hidden-item-2 (nth activity-items 1)]
                    (tu/test-string-contains (dommy/class hidden-item-1) "hidden-new-activity")
                    (tu/test-string-contains (dommy/class hidden-item-2) "hidden-new-activity")

                    (tu/click! :.func--reveal-new-activities__link)

                    (tu/test-string-does-not-contain (dommy/class hidden-item-1) "hidden-new-activity")
                    (tu/test-string-does-not-contain (dommy/class hidden-item-2) "hidden-new-activity")

                    (is (= 0 @feed/number-of-hidden-activities))))
         (testing "reveal activities link is hidden after it is clicked"
                  (set-initial-state)
                  (feed/newer-activities-handler (constantly nil) (html-response-hidden))
                  (tu/click! :.func--reveal-new-activities__link)
                  (tu/test-string-does-not-contain (dommy/class (sel1 :.func--reveal-new-activities__link)) "show-new-activities__link"))
         (testing "Date is converted to human readable time"
                  (set-initial-state)
                  (feed/newer-activities-handler (constantly nil) (html-response-hidden))
                  (is (= (dommy/text (sel1 :.clj--activity-item__time)) "3 years ago"))))

(deftest about-validation-on-responses
         (testing "invalid response is not added to the feed"
                  (set-initial-state)
                  (let [html (dommy/html (sel1 :html))]
                    (feed/newer-activities-handler (constantly nil) invalid-html-response)
                    (is (= html (dommy/html (sel1 :html))))
                    (feed/older-activities-handler (constantly nil) invalid-html-response)
                    (is (= html (dommy/html (sel1 :html))))))
         (testing "invalid response stops the polling for new activities"
                  (set-initial-state)
                  (let [handler-result (feed/newer-activities-handler (constantly true) invalid-html-response)]
                    (is (nil? handler-result)))))
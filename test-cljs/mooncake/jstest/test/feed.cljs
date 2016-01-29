(ns mooncake.jstest.test.feed
  (:require [cemerick.cljs.test]
            [dommy.core :as d]
            [mooncake.js.feed :as feed]
            [mooncake.jstest.test-utils :as tu]
            [dommy.core :as dommy]
            [mooncake.js.dom :as dom])
  (:require-macros [cemerick.cljs.test :refer [deftest is testing]]
                   [mooncake.jstest.macros :refer [load-template]]
                   [dommy.core :as dm]))

(defonce feed-page-template (load-template "public/feed.html"))

(def invalid-html-response "<a></a><li></li>")
(def valid-html-response "<li></li><li><a></a></li>")

(defn set-initial-state []
      (tu/set-html! feed-page-template))

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
                  (feed/newer-activities-error-handler {})
                  (tu/test-string-contains (d/class (dm/sel1 :.clj--new-activities__error)) "show-feed-activities__error")))

(deftest adds-spinner-before-adding-old-activities
         (testing "spinner appears on load-old-activities"
                  (set-initial-state)
                  (is (not (nil? (d/attr (dm/sel1 :.clj--activity-loading-spinner) "hidden"))))
                  (feed/load-older-activities (constantly {}))
                  (is (= (d/attr (dm/sel1 :.clj--activity-loading-spinner) "hidden") nil))))

(deftest removes-spinner-after-adding-old-activities
         (testing "spinner gets removed when activities are appended"
                  (set-initial-state)
                  (feed/load-older-activities (constantly {}))
                  (feed/append-older-activities (constantly {}) "")
                  (is (not (nil? (d/attr (dm/sel1 :.clj--activity-loading-spinner) "hidden"))))))

(deftest about-validating-the-response
         (testing "valid-response returns whether all top-level elements in the fragment are li"
                  (set-initial-state)
                  (is (feed/valid-response? valid-html-response))
                  (is (not (feed/valid-response? invalid-html-response)))))

(deftest about-concurrent-requests
         (testing "request not made if another is in progress"
                  (set-initial-state)
                  (reset! feed/request-not-in-progress false)
                  (is (nil? (feed/load-older-activities (constantly nil)))))
         (testing "request-not-in-progress set to false as request is made"
                  (set-initial-state)
                  (reset! feed/request-not-in-progress true)
                  (feed/load-older-activities (constantly nil))
                  (is (false? @feed/request-not-in-progress)))
         (testing "request-not-in-progress set to true as empty response is handled"
                  (set-initial-state)
                  (is (false? @feed/request-not-in-progress))
                  (feed/older-activities-handler (constantly nil) "")
                  (is (true? @feed/request-not-in-progress)))
         (testing "request-not-in-progress set to true after activities appended"
                  (set-initial-state)
                  (reset! feed/request-not-in-progress false)
                  (feed/append-older-activities (constantly nil) valid-html-response)
                  (is (true? @feed/request-not-in-progress))))

(deftest about-handling-the-response
         (testing "invalid response removes the scroll listener and hides the spinner"
                  (set-initial-state)
                  (feed/older-activities-handler feed/load-more-activities-if-at-end-of-page invalid-html-response)
                  (is (empty? (dommy/event-listeners js/window)))
                  (is (not (nil? (d/attr (dm/sel1 :.clj--activity-loading-spinner) "hidden")))))
         (testing "empty response removes the scroll listener and hides the spinner"
                  (set-initial-state)
                  (feed/older-activities-handler feed/load-more-activities-if-at-end-of-page "")
                  (is (empty? (dommy/event-listeners js/window)))
                  (is (not (nil? (d/attr (dm/sel1 :.clj--activity-loading-spinner) "hidden")))))
         (testing "valid response calls the load-activities-fn after appending activities"
                  (set-initial-state)
                  (is (true? (feed/append-older-activities (constantly true) valid-html-response)))))

(deftest about-handling-client-side-translations
         (testing "if given an unsupported language then falls back to english"
                  (with-redefs [dom/get-lang (constantly "en")]
                               (is (= (feed/new-activities-link-text 2) "View 2 new activities"))))
         (testing "if lang atom is updated then returns a translated message"
                  (with-redefs [dom/get-lang (constantly "en")]
                    (is (not (contains? (feed/new-activities-link-text 1) "Finnish"))))
                  (with-redefs [dom/get-lang (constantly "fi")]
                               (is (= (feed/new-activities-link-text 1) "View in Finnish 1 new activity in Finnish")))))

(deftest about-converting-the-activity-time-to-readable-format
         (testing "the time is converted to English by default"
                  (set-initial-state)
                  (feed/give-all-activities-human-readable-time)
                  (is (= (d/text (dm/sel1 :.clj--activity-item__time)) "5 months ago")))
         (testing "the time is in the same language as the browser"
                  (set-initial-state)
                  (tu/set-lang! "fi")
                  (feed/give-all-activities-human-readable-time)
                  (is (= (d/text (dm/sel1 :.clj--activity-item__time)) "viisi kuukautta sitten"))))
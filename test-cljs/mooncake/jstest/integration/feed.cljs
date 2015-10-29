(ns mooncake.jstest.integration.feed
  (:require [cemerick.cljs.test]
            [mooncake.jstest.test-utils :as tu]
            [mooncake.js.app :as app])
  (:require-macros [cemerick.cljs.test :refer [deftest is testing]]
                   [mooncake.jstest.macros :refer [load-template]]
                   [dommy.core :refer [sel1 sel]]))

(defonce feed-page-template (load-template "public/feed.html"))

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

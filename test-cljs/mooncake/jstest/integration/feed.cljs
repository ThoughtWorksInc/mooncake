(ns mooncake.jstest.integration.feed
  (:require [cemerick.cljs.test]
            [mooncake.jstest.test-utils :as tu]
            [mooncake.js.app :as app])
  (:require-macros [cemerick.cljs.test :refer [deftest is testing]]
                   [mooncake.jstest.macros :refer [load-template]]))

(defonce feed-page-template (load-template "public/feed.html"))

(defn set-initial-state []
  (tu/set-html! feed-page-template)
  (app/start))

(deftest about-loading-more-activities
         (testing "Can render loading of more activities"
                  (set-initial-state)

                  ))

(ns mooncake.jstest.integration.customise-feed
  (:require [cemerick.cljs.test]
            [mooncake.jstest.test-utils :as tu]
            [mooncake.js.app :as app])
  (:require-macros [cemerick.cljs.test :refer [deftest is testing]]
                   [mooncake.jstest.macros :refer [load-template]]))

(defonce customise-feed-page-template (load-template "public/customise-feed.html"))

; RS + AW + AB | 30 Sept 2015 | WIP - will be removed tomorrow
#_(deftest checking-a-type-checkbox-sets-source-checkbox-to-the-some-state
         (tu/set-html! customise-feed-page-template)
         (app/start)
         (tu/remove-attribute-from-all :.clj--feed-item-child__checkbox :checked)
         (tu/test-field-has-class :.clj--src-checkbox :checkbox--off)
         (tu/test-field-doesnt-have-class :.clj--src-checkbox :checkbox--on)
         (tu/test-field-doesnt-have-class :.clj--src-checkbox :checkbox--some)
         (tu/check! "#objective8_-_Create")
         (tu/test-field-has-class :.clj--src-checkbox :checkbox--some)
         (tu/test-field-doesnt-have-class :.clj--src-checkbox :checkbox--on)
         (tu/test-field-doesnt-have-class :.clj--src-checkbox :checkbox--off))

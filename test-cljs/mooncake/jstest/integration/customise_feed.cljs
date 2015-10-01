(ns mooncake.jstest.integration.customise-feed
  (:require [cemerick.cljs.test]
            [mooncake.jstest.test-utils :as tu]
            [mooncake.js.app :as app])
  (:require-macros [cemerick.cljs.test :refer [deftest is testing]]
                   [mooncake.jstest.macros :refer [load-template]]))

(def checkbox-none-class :checkbox--none)
(def checkbox-some-class :checkbox--some)
(def checkbox-all-class :checkbox--all)

(defonce customise-feed-page-template (load-template "public/customise-feed.html"))

(deftest about-setting-src-checkbox-class
         (testing "checking a type checkbox sets src checkbox to the 'some' state"
                  (tu/set-html! customise-feed-page-template)
                  (app/start)
                  (tu/remove-attribute-from-all :.clj--feed-item-child__checkbox :checked)
                  (tu/test-field-has-class :.clj--src-checkbox checkbox-none-class)
                  (tu/test-field-doesnt-have-class :.clj--src-checkbox checkbox-some-class)
                  (tu/test-field-doesnt-have-class :.clj--src-checkbox checkbox-all-class)
                  (tu/check! :#objective8_-_Create)
                  (tu/test-field-has-class :.clj--src-checkbox checkbox-some-class)
                  (tu/test-field-doesnt-have-class :.clj--src-checkbox checkbox-all-class)
                  (tu/test-field-doesnt-have-class :.clj--src-checkbox checkbox-none-class))
         (testing "checking all type checkboxes sets src checkbox to the 'all' state"
                  (tu/set-html! customise-feed-page-template)
                  (app/start)
                  (tu/remove-attribute-from-all :.clj--feed-item-child__checkbox :checked)
                  (tu/test-field-has-class :.clj--src-checkbox checkbox-none-class)
                  (tu/test-field-doesnt-have-class :.clj--src-checkbox checkbox-some-class)
                  (tu/test-field-doesnt-have-class :.clj--src-checkbox checkbox-all-class)
                  (tu/set-attr! :#objective8_-_Create :checked "checked")
                  (tu/set-attr! :#objective8_-_Comment :checked "checked")
                  (tu/check! :#objective8_-_Question)
                  (tu/test-field-has-class :.clj--src-checkbox checkbox-all-class)
                  (tu/test-field-doesnt-have-class :.clj--src-checkbox checkbox-some-class)
                  (tu/test-field-doesnt-have-class :.clj--src-checkbox checkbox-none-class)))

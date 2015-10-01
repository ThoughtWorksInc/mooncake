(ns mooncake.jstest.integration.customise-feed
  (:require [cemerick.cljs.test]
            [mooncake.jstest.test-utils :as tu]
            [mooncake.js.app :as app])
  (:require-macros [cemerick.cljs.test :refer [deftest is testing]]
                   [mooncake.jstest.macros :refer [load-template]]))

(def checkbox-none-class :checkbox--none)
(def checkbox-some-class :checkbox--some)
(def checkbox-all-class :checkbox--all)

(def objective8-src-checkbox-selector [:.clj--feed-item:first-of-type :.clj--src-checkbox])
(def tflupdates-src-checkbox-selector [:.clj--feed-item:last-of-type :.clj--src-checkbox])

(defonce customise-feed-page-template (load-template "public/customise-feed.html"))

(defn test-src-checkbox-in-state [src-checkbox-selector state]
  (case state
    :all (do (tu/test-field-has-class src-checkbox-selector checkbox-all-class)
             (tu/test-field-doesnt-have-class src-checkbox-selector checkbox-some-class)
             (tu/test-field-doesnt-have-class src-checkbox-selector checkbox-none-class))
    :some (do (tu/test-field-has-class src-checkbox-selector checkbox-some-class)
              (tu/test-field-doesnt-have-class src-checkbox-selector checkbox-all-class)
              (tu/test-field-doesnt-have-class src-checkbox-selector checkbox-none-class))
    :none (do (tu/test-field-has-class src-checkbox-selector checkbox-none-class)
              (tu/test-field-doesnt-have-class src-checkbox-selector checkbox-all-class)
              (tu/test-field-doesnt-have-class src-checkbox-selector checkbox-some-class))))

(defn test-tflupdates-src-checkbox-in-none-state []
  (test-src-checkbox-in-state tflupdates-src-checkbox-selector :none))

(defn test-tflupdates-src-checkbox-in-some-state []
  (test-src-checkbox-in-state tflupdates-src-checkbox-selector :some))

(defn test-objective8-src-checkbox-in-none-state []
  (test-src-checkbox-in-state objective8-src-checkbox-selector :none))

(defn test-objective8-src-checkbox-in-some-state []
  (test-src-checkbox-in-state objective8-src-checkbox-selector :some))

(defn test-objective8-src-checkbox-in-all-state []
  (test-src-checkbox-in-state objective8-src-checkbox-selector :all))

(defn set-all-unchecked-initial-state []
  (tu/set-html! customise-feed-page-template)
  (app/start)
  (tu/remove-attr-from-all! :.clj--feed-item-child__checkbox :checked))

(deftest about-setting-src-checkbox-class
         (testing "when all type checkboxes are unchecked - checking a type checkbox sets src checkbox to the 'some' state"
                  (set-all-unchecked-initial-state)
                  (test-objective8-src-checkbox-in-none-state)

                  (tu/check! :#objective8_-_Create)
                  (test-objective8-src-checkbox-in-some-state))

         (testing "when all but one type checkbox are checked - checking the last type checkbox sets src checkbox to the 'all' state"
                  (set-all-unchecked-initial-state)
                  (tu/add-class! objective8-src-checkbox-selector checkbox-some-class)
                  (tu/remove-class! objective8-src-checkbox-selector checkbox-none-class)
                  (test-objective8-src-checkbox-in-some-state)

                  (tu/set-attr! :#objective8_-_Create :checked "checked")
                  (tu/set-attr! :#objective8_-_Comment :checked "checked")
                  (tu/check! :#objective8_-_Question)
                  (test-objective8-src-checkbox-in-all-state))

         (testing "when all but one type checkbox are unchecked - unchecking the last checkbox sets src checkbox to the 'none' state"
                  (set-all-unchecked-initial-state)
                  (tu/add-class! objective8-src-checkbox-selector checkbox-some-class)
                  (tu/remove-class! objective8-src-checkbox-selector checkbox-none-class)
                  (test-objective8-src-checkbox-in-some-state)

                  (tu/remove-attr! :#objective8_-_Create :checked)
                  (tu/remove-attr! :#objective8_-_Comment :checked)
                  (tu/uncheck! :#objective8_-_Question)
                  (test-objective8-src-checkbox-in-none-state))

         (testing "checking a type checkbox does not affect the src checkbox for a different source"
                  (set-all-unchecked-initial-state)
                  (test-objective8-src-checkbox-in-none-state)
                  (test-tflupdates-src-checkbox-in-none-state)

                  (tu/check! :#tflupdates_-_Bus)
                  (test-objective8-src-checkbox-in-none-state)
                  (test-tflupdates-src-checkbox-in-some-state)))

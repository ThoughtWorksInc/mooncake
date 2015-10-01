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

(def all-objective8-type-checkboxes-selector [:.clj--feed-item:first-of-type :.clj--feed-item-child__checkbox])

(defonce customise-feed-page-template (load-template "public/customise-feed.html"))

(defn test-src-checkbox-in-state [src-checkbox-selector state]
  (case state
    :all (do (tu/test-field-has-class src-checkbox-selector checkbox-all-class)
             (tu/test-field-does-not-have-class src-checkbox-selector checkbox-some-class)
             (tu/test-field-does-not-have-class src-checkbox-selector checkbox-none-class))
    :some (do (tu/test-field-has-class src-checkbox-selector checkbox-some-class)
              (tu/test-field-does-not-have-class src-checkbox-selector checkbox-all-class)
              (tu/test-field-does-not-have-class src-checkbox-selector checkbox-none-class))
    :none (do (tu/test-field-has-class src-checkbox-selector checkbox-none-class)
              (tu/test-field-does-not-have-class src-checkbox-selector checkbox-all-class)
              (tu/test-field-does-not-have-class src-checkbox-selector checkbox-some-class))))

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

(defn test-all-checked [checkboxes-selector]
  (doseq [checkbox (tu/select-all checkboxes-selector)]
    (tu/test-checked checkbox)))

(defn test-none-checked [checkboxes-selector]
  (doseq [checkbox (tu/select-all checkboxes-selector)]
    (tu/test-unchecked checkbox)))

(defn set-initial-state []
  (tu/set-html! customise-feed-page-template)
  (app/start))

(defn set-all-checked-initial-state []
  (set-initial-state)
  (tu/check-all-without-firing-change-event! :.clj--feed-item-child__checkbox))

(defn set-some-checked-initial-state []
  (set-initial-state)
  (tu/check-all-without-firing-change-event! :.clj--feed-item-child__checkbox)
  (tu/uncheck-without-firing-change-event! :.clj--feed-item-child__checkbox))

(defn set-none-checked-initial-state []
  (set-initial-state)
  (tu/uncheck-all-without-firing-change-event! :.clj--feed-item-child__checkbox))

(deftest about-type-checkboxes-setting-src-checkbox-class
         (testing "when all type checkboxes are unchecked - checking a type checkbox sets src checkbox to the 'some' state"
                  (set-none-checked-initial-state)
                  (test-objective8-src-checkbox-in-none-state)

                  (tu/check! :#objective8_-_Create)
                  (test-objective8-src-checkbox-in-some-state))

         (testing "when all but one type checkbox are checked - checking the last type checkbox sets src checkbox to the 'all' state"
                  (set-none-checked-initial-state)
                  (tu/add-class! objective8-src-checkbox-selector checkbox-some-class)
                  (tu/remove-class! objective8-src-checkbox-selector checkbox-none-class)
                  (test-objective8-src-checkbox-in-some-state)

                  (tu/check-without-firing-change-event! :#objective8_-_Create)
                  (tu/check-without-firing-change-event! :#objective8_-_Comment)
                  (tu/check! :#objective8_-_Question)
                  (test-objective8-src-checkbox-in-all-state))

         (testing "when all but one type checkbox are unchecked - unchecking the last checkbox sets src checkbox to the 'none' state"
                  (set-none-checked-initial-state)
                  (tu/add-class! objective8-src-checkbox-selector checkbox-some-class)
                  (tu/remove-class! objective8-src-checkbox-selector checkbox-none-class)
                  (test-objective8-src-checkbox-in-some-state)

                  (tu/uncheck-without-firing-change-event! :#objective8_-_Create)
                  (tu/uncheck-without-firing-change-event! :#objective8_-_Comment)
                  (tu/uncheck! :#objective8_-_Question)
                  (test-objective8-src-checkbox-in-none-state))

         (testing "checking a type checkbox does not affect the src checkbox for a different source"
                  (set-none-checked-initial-state)
                  (test-objective8-src-checkbox-in-none-state)
                  (test-tflupdates-src-checkbox-in-none-state)

                  (tu/check! :#tflupdates_-_Bus)
                  (test-objective8-src-checkbox-in-none-state)
                  (test-tflupdates-src-checkbox-in-some-state)))

(deftest about-src-checkbox-changing-type-checkbox-values
         (testing "when src-checkbox is in 'none' state, clicking src-checkbox"
                  (set-none-checked-initial-state)
                  (test-objective8-src-checkbox-in-none-state)
                  (test-none-checked all-objective8-type-checkboxes-selector)

                  (tu/click! objective8-src-checkbox-selector)
                  (testing "- changes the state to 'all'"
                           (test-objective8-src-checkbox-in-all-state))
                  (testing "- checks all type checkboxes"
                           (test-all-checked all-objective8-type-checkboxes-selector)))

         (testing "when src-checkbox is in 'all' state, clicking src-checkbox"
                  (set-all-checked-initial-state)
                  (tu/add-class! objective8-src-checkbox-selector checkbox-all-class)
                  (tu/remove-class! objective8-src-checkbox-selector checkbox-none-class)
                  (test-objective8-src-checkbox-in-all-state)
                  (test-all-checked all-objective8-type-checkboxes-selector)

                  (tu/click! objective8-src-checkbox-selector)
                  (testing "- changes the state to 'none'"
                           (test-objective8-src-checkbox-in-none-state))
                  (testing "- unchecks all type checkboxes"
                           (test-none-checked all-objective8-type-checkboxes-selector)))

         (testing "when src-checkbox is in 'some' state, clicking src-checkbox"
                  (set-some-checked-initial-state)
                  (tu/add-class! objective8-src-checkbox-selector checkbox-some-class)
                  (tu/remove-class! objective8-src-checkbox-selector checkbox-none-class)
                  (test-objective8-src-checkbox-in-some-state)

                  (tu/click! objective8-src-checkbox-selector)
                  (testing "- changes the state to 'none'"
                           (test-objective8-src-checkbox-in-none-state))
                  (testing "- unchecks all type checkboxes"
                           (test-none-checked all-objective8-type-checkboxes-selector)))

         (testing "clicking a src-checkbox does not affect unrelated elements"
                  (set-none-checked-initial-state)
                  (test-objective8-src-checkbox-in-none-state)
                  (test-tflupdates-src-checkbox-in-none-state)
                  (test-none-checked all-objective8-type-checkboxes-selector)

                  (tu/click! tflupdates-src-checkbox-selector)
                  (testing "- unrelated source checkbox stays in the same state"
                           (test-objective8-src-checkbox-in-none-state))
                  (testing "- unrelated type checkboxes are unchanged"
                           (test-none-checked all-objective8-type-checkboxes-selector))))

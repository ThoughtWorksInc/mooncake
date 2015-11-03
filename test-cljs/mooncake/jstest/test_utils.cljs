(ns mooncake.jstest.test-utils
  (:require [cemerick.cljs.test]
            [dommy.core :as dommy])
  (:require-macros [cemerick.cljs.test :refer [deftest is testing run-tests]]
                   [dommy.core :refer [sel1 sel]]))

(defn print-element
  ([message e]
   (print (str message ": " (.-outerHTML e)))
   e)
  ([e] (print-element "" e)))

(defn print-html []
  (print (dommy/html (sel1 :html))))

;; Assertions

(defn test-field-class-existance [has-class? selector css-class]
  (is (= has-class? (dommy/has-class? (sel1 selector) css-class))
      (if has-class?
        (str "field: " selector " does not contain expected class: " css-class)
        (str "field: " selector " contains class " css-class " when it shouldn't"))))

(def test-field-does-not-have-class (partial test-field-class-existance false))
(def test-field-has-class (partial test-field-class-existance true))

(defn test-checked [elem]
  (is (= true (.-checked elem))))

(defn test-unchecked [elem]
  (is (= false (.-checked elem))))

(defn test-string-contains [str s]
  (is (not= -1 (.indexOf str s))))

(defn test-string-does-not-contain [str s]
  (is (= -1 (.indexOf str s))))


;; Getters and setters

(defn set-html! [html-string]
  (dommy/set-html! (sel1 :html) html-string))

(defn select-all [selector]
  (sel selector))

(defn add-class! [selector class]
  (dommy/add-class! (sel1 selector) class))

(defn remove-class! [selector class]
  (dommy/remove-class! (sel1 selector) class))


;; Events

(defn create-event [event-type]
  (let [event (.createEvent js/document "Event")]
    (.initEvent event (name event-type) true true)
    event))

(defn fire!
  "Creates an event of type `event-type`, optionally having
   `update-event!` mutate and return an updated event object,
   and fires it on `node`.
   Only works when `node` is in the DOM"
  [node event-type & [update-event!]]
  (let [update-event! (or update-event! identity)]
    (if (.-createEvent js/document)
      (let [event (create-event event-type)]
        (.dispatchEvent node (update-event! event)))
      (.fireEvent node (str "on" (name event-type))
                  (update-event! (.createEventObject js/document))))))
(defn click! [selector]
  (fire! (sel1 selector) :click))

(defn change! [selector]
  (fire! (sel1 selector) :change))

(defn check-one-without-firing-change-event! [selector]
  (set! (.-checked (sel1 selector)) true))

(defn check-all-without-firing-change-event! [selector]
  (doseq [elem (sel selector)]
    (set! (.-checked elem) true)))

(defn uncheck-one-without-firing-change-event! [selector]
  (set! (.-checked (sel1 selector)) false))

(defn uncheck-all-without-firing-change-event! [selector]
  (doseq [elem (sel selector)]
    (set! (.-checked elem) false)))

(defn check! [selector]
  (check-one-without-firing-change-event! selector)
  (change! selector))

(defn uncheck! [selector]
  (uncheck-one-without-firing-change-event! selector)
  (change! selector))
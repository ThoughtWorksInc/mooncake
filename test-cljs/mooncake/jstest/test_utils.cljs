(ns mooncake.jstest.test-utils
  (:require [cemerick.cljs.test]
            [dommy.core :as dommy]
            [clojure.string :as string])
  (:require-macros [cemerick.cljs.test :refer [deftest is testing run-tests]]
                   [dommy.core :refer [sel1 sel]]))

(defn print-element
  ([message e]
   (print (str message ": " (.-outerHTML e)))
   e)
  ([e] (print-element "" e)))

(defn print-html []
  (print (dommy/html (sel1 :html))))

(defn set-html! [html-string]
  (dommy/set-html! (sel1 :html) html-string))

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

(defn set-value [sel text]
  (dommy/set-value! (sel1 sel) text))

(defn test-field-class-existance [has-class? selector css-class]
  (is (= has-class? (dommy/has-class? (sel1 selector) css-class))
      (if has-class?
        (str "field: " selector " does not contain expected class: " css-class)
        (str "field: " selector " contains class " css-class " when it shouldn't"))))

(def test-field-doesnt-have-class (partial test-field-class-existance false))
(def test-field-has-class (partial test-field-class-existance true))

(defn remove-attribute-from-all [selector attr]
  (doseq [elem (sel selector)]
      (dommy/remove-attr! elem attr)))

(defn change! [selector]
  (fire! (sel1 selector) :change))

(defn check! [selector]
  (dommy/set-attr! (sel1 selector) :checked)
  (change! selector))
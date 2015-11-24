(ns mooncake.js.dom
  (:require [dommy.core :as d])
  (:require-macros [dommy.core :as dm]
                   [mooncake.translation :as t]))

(defn get-scroll-top []
  (let [document-element (.-documentElement js/document)
        document-body (.-body js/document)]
    (max (and document-body (.-scrollTop document-body)) (.-scrollTop document-element))))

(defn get-scroll-height []
  (let [document-element (.-documentElement js/document)
        document-body (.-body js/document)]
    (or (and document-element (.-scrollHeight document-element)) (.-scrollHeight document-body))))

(defn remove-if-present! [selector]
  (when-let [e (dm/sel1 selector)]
    (d/remove! e)))

(defn add-if-not-present [selector class]
  (when (not (d/has-class? selector class))
    (d/add-class! selector class)))

(defn get-lang []
  (keyword (.getAttribute (dm/sel1 :html) "lang")))

(defn string-contains [str s]
  (not= -1 (.indexOf str s)))

(defn body-has-class? [class-str]
  (string-contains (d/class (dm/sel1 :body)) class-str))



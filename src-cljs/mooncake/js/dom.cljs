(ns mooncake.js.dom
  (:require [dommy.core :as d])
  (:require-macros [dommy.core :as dm]))

(defn get-page-attr [attr]
  (let [document-element (.-documentElement js/document)
        document-body (.-body js/document)]
    (or (and document-element (aget document-element attr)) (aget document-body attr))))

(defn scroll-amount []
  (get-page-attr "scrollTop"))

(defn page-length []
  (get-page-attr "scrollHeight"))

(defn remove-if-present! [selector]
  (when-let [e (dm/sel1 selector)]
    (d/remove! e)))

(defn string-contains [str s]
  (not= -1 (.indexOf str s)))

(defn body-has-class? [class-str]
  (string-contains (d/class (dm/sel1 :body)) class-str))
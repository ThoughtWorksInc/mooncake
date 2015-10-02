(ns mooncake.js.customise-feed
  (:require [dommy.core :as d])
  (:require-macros [dommy.core :as dm]))


(def checkbox-none-class :checkbox--none)
(def checkbox-some-class :checkbox--some)
(def checkbox-all-class :checkbox--all)

(def source-container-selector :.clj--feed-item)
(def src-checkbox-selector :.clj--src-checkbox)
(def type-checkbox-selector :.clj--feed-item-child__checkbox)

(defn- get-src-checkbox-state [src-checkbox-elem]
  (cond
    (d/has-class? src-checkbox-elem checkbox-all-class) :all
    (d/has-class? src-checkbox-elem checkbox-some-class) :some
    :default :none))

(defn- set-src-checkbox-state! [src-checkbox-elem state]
  (d/remove-class! src-checkbox-elem checkbox-all-class checkbox-some-class checkbox-none-class)
  (d/add-class! src-checkbox-elem (case state :all checkbox-all-class
                                              :some checkbox-some-class
                                              :none checkbox-none-class)))

(defn- set-all-checked-state! [src-checkbox-elem type-checkbox-elems]
  (do (doseq [checkbox type-checkbox-elems]
        (set! (.-checked checkbox) true))
      (set-src-checkbox-state! src-checkbox-elem :all)))

(defn- set-none-checked-state! [src-checkbox-elem type-checkbox-elems]
  (do (doseq [checkbox type-checkbox-elems]
        (set! (.-checked checkbox) false))
      (set-src-checkbox-state! src-checkbox-elem :none)))

(defn type-checkbox-changed [e]
  (let [feed-item-elem (d/closest (.-target e) source-container-selector)
        type-checkbox-elems (dm/sel feed-item-elem type-checkbox-selector)
        checked-attrs (map #(.-checked %) type-checkbox-elems)
        src-checkbox-elem (dm/sel1 feed-item-elem src-checkbox-selector)]
    (cond
      (every? true? checked-attrs) (set-src-checkbox-state! src-checkbox-elem :all)
      (every? false? checked-attrs) (set-src-checkbox-state! src-checkbox-elem :none)
      :else (set-src-checkbox-state! src-checkbox-elem :some))))

(defn src-checkbox-clicked [e]
  (let [src-checkbox-elem (.-target e)
        feed-item-elem (d/closest src-checkbox-elem source-container-selector)
        type-checkbox-elems (dm/sel feed-item-elem type-checkbox-selector)]
    (case (get-src-checkbox-state src-checkbox-elem)
      :all (set-none-checked-state! src-checkbox-elem type-checkbox-elems)
      :some (set-none-checked-state! src-checkbox-elem type-checkbox-elems)
      :none (set-all-checked-state! src-checkbox-elem type-checkbox-elems))))

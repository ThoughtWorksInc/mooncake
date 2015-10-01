(ns mooncake.js.app
  (:require [dommy.core :as d])
  (:require-macros [dommy.core :as dm]))

(def checkbox-none-class :checkbox--none)
(def checkbox-some-class :checkbox--some)
(def checkbox-all-class :checkbox--all)

(defn- set-src-checkbox-state [src-checkbox-elem state]
  (d/remove-class! src-checkbox-elem checkbox-all-class checkbox-some-class checkbox-none-class)
  (d/add-class! src-checkbox-elem (case state :all checkbox-all-class
                                              :some checkbox-some-class
                                              :none checkbox-none-class)))

(defn checkbox-changed [e]
  (let [feed-item-elem (d/closest (.-target e) :.clj--feed-item)
        type-checkbox-elems (dm/sel feed-item-elem :.clj--feed-item-child__checkbox)
        checked-attrs (map #(d/attr % :checked) type-checkbox-elems)
        src-checkbox-elem (dm/sel1 feed-item-elem :.clj--src-checkbox)]
    (cond
      (every? identity checked-attrs) (set-src-checkbox-state src-checkbox-elem :all)
      (every? nil? checked-attrs) (set-src-checkbox-state src-checkbox-elem :none)
      :else (set-src-checkbox-state src-checkbox-elem :some))))

(defn setup-multi-listeners [selector event function]
  (when-let [elems (dm/sel selector)]
    (doseq [elem elems] (d/listen! elem event function))))

(defn start []
  (setup-multi-listeners :.clj--feed-item-child__checkbox :change checkbox-changed))

(set! (.-onload js/window) start)
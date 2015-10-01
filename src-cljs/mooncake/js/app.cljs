(ns mooncake.js.app
  (:require [dommy.core :as d])
  (:require-macros [dommy.core :as dm]))

(defn checkbox-changed [e]
  (let [feed-item-elem (-> (.-target e)
                           (d/closest :.clj--feed-item))
        type-checkbox-elems (dm/sel feed-item-elem :.clj--feed-item-child__checkbox)
        checked-attrs (map #(d/attr % :checked) type-checkbox-elems)
        src-checkbox-elem (-> feed-item-elem
                              (dm/sel1 :.clj--src-checkbox))]
    (if (every? identity checked-attrs)
      (do (d/remove-class! src-checkbox-elem :checkbox--none)
          (d/add-class! src-checkbox-elem :checkbox--all))
      (do (d/remove-class! src-checkbox-elem :checkbox--none)
          (d/add-class! src-checkbox-elem :checkbox--some)))))

(defn setup-multi-listeners [selector event function]
  (when-let [elems (dm/sel selector)]
    (doseq [elem elems] (d/listen! elem event function))))

(defn start []
  (setup-multi-listeners :.clj--feed-item-child__checkbox :change checkbox-changed))

(set! (.-onload js/window) start)
(ns mooncake.js.app
  (:require [dommy.core :as d])
  (:require-macros [dommy.core :as dm]))

(defn checkbox-changed [e]
  (let [src-checkbox-elem (-> (.-target e)
                              (d/closest :.clj--feed-item)
                              (dm/sel1 :.clj--src-checkbox))]
    (d/remove-class! src-checkbox-elem :checkbox--off)
    (d/add-class! src-checkbox-elem :checkbox--some)))

(defn setup-multi-listeners [selector event function]
  (when-let [elems (dm/sel selector)]
    (doseq [elem elems] (d/listen! elem event function))))

(defn start []
  (setup-multi-listeners :.clj--feed-item-child__checkbox :change checkbox-changed))

(set! (.-onload js/window) start)
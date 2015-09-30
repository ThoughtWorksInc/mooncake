(ns mooncake.js.app
  (:require [dommy.core :as d])
  (:require-macros [dommy.core :as dm]))

(defn checkbox-changed [e]
  (print "E: " (.-id (.-target e))))

(defn setup-multi-listeners [selector event function]
  (when-let [elems (dm/sel selector)]
    (doseq [elem elems] (d/listen! elem event function))))

(defn start []
  (setup-multi-listeners :.clj--feed-item-child__checkbox :change checkbox-changed))

(set! (.-onload js/window) start)
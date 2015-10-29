(ns mooncake.js.app
  (:require [dommy.core :as d]
            [mooncake.js.customise-feed :as cf]
            [mooncake.js.feed :as f])
  (:require-macros [dommy.core :as dm]))

(defn setup-listener [selector event function]
  (when-let [e (dm/sel1 selector)]
    (d/listen! e event function)))

(defn setup-multi-listeners [selector event function]
  (when-let [elems (dm/sel selector)]
    (doseq [elem elems] (d/listen! elem event function))))

(defn start []
  (f/hide-pagination-buttons)
  (setup-listener f/load-older-activities-link :click f/load-older-activities)
  (setup-multi-listeners cf/type-checkbox-selector :change cf/type-checkbox-changed)
  (setup-multi-listeners cf/src-checkbox-selector :click cf/src-checkbox-clicked))

(set! (.-onload js/window) start)
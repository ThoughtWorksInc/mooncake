(ns mooncake.js.app
  (:require [dommy.core :as d]
            [mooncake.js.customise-feed :as cf])
  (:require-macros [dommy.core :as dm]))


(defn setup-multi-listeners [selector event function]
  (when-let [elems (dm/sel selector)]
    (doseq [elem elems] (d/listen! elem event function))))

(defn start []
  (setup-multi-listeners cf/type-checkbox-selector :change cf/type-checkbox-changed)
  (setup-multi-listeners cf/src-checkbox-selector :click cf/src-checkbox-clicked))

(set! (.-onload js/window) start)
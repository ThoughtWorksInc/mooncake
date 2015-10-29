(ns mooncake.js.dom
  (:require [dommy.core :as d])
  (:require-macros [dommy.core :as dm]))

(defn remove-if-present! [selector]
  (when-let [e (dm/sel1 selector)]
    (d/remove! e)))
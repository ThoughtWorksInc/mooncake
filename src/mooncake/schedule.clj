(ns mooncake.schedule
  (:require [chime :as chime]
            [clj-time.core :as t]
            [clj-time.periodic :as pt]))

(defn get-intervals [interval-secs]
  (pt/periodic-seq (t/now)
                   (-> interval-secs t/seconds)))

(defn schedule [f interval-secs]
  (chime/chime-at (get-intervals interval-secs) f))
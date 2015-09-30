(ns mooncake.jstest.macros
  (:require [clojure.java.io :as io]))

(defmacro load-template [r]
  "Reads and returns a template as a string."
  (slurp (io/resource r)))



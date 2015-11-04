(ns mooncake.js.config
  (:require [environ.core :as env]))

(defmacro polling-interval-ms []
  (or (:polling-interval env/env) 8000))

(ns mooncake.routes
  (:require [clojure.tools.logging :as log]
            [scenic.routes :as scenic]
            [bidi.bidi :as bidi]
            [mooncake.config :as c]))

(def routes (scenic/load-routes-from-file "routes.txt"))

(defn path [action & params]
  (try
    (apply bidi/path-for routes action params)
    (catch clojure.lang.ArityException e
      (log/warn (format "Key: '%s' probably does not match a route.\n%s" action e))
      (throw (Exception. (format "Error constructing url for action '%s', with params '%s'" action params))))))

(defn absolute-path [config-m action & params]
  (str (c/base-url config-m) (apply path action params)))

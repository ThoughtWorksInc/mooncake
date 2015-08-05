(ns mooncake.handler
  (:require [scenic.routes :as scenic]
            [ring.adapter.jetty :as ring-jetty]
            [ring.util.response :as r]
            [mooncake.routes :as routes]
            [mooncake.config :as config]))

(defn index [request]
  (r/response "test"))

(def site-handlers
  {:index index})

(defn create-app [config-m]
  (scenic/scenic-handler routes/routes site-handlers))

(def app (create-app (config/create-config)))

(defn -main [& args]
  (let [config-m (config/create-config)]
    (ring-jetty/run-jetty app {:port (config/port config-m)
                               :host (config/host config-m)})))

(ns mooncake.handler
  (:require [scenic.routes :as scenic]
            [ring.adapter.jetty :as ring-jetty]
            [ring.util.response :as r]
            [ring.middleware.defaults :as ring-mw]
            [mooncake.routes :as routes]
            [mooncake.config :as config]))

(defn index [request]
  (-> (r/response "test 2")
      (r/content-type "text/plain")))

(def site-handlers
  {:index index})

(defn wrap-defaults-config [secure?]
  (-> (if secure? (assoc ring-mw/secure-site-defaults :proxy true) ring-mw/site-defaults)
      (assoc-in [:session :cookie-attrs :max-age] 3600)
      (assoc-in [:session :cookie-name] "mooncake-session")))

(defn create-app [config-m]
  (-> (scenic/scenic-handler routes/routes site-handlers)
      (ring-mw/wrap-defaults (wrap-defaults-config (config/secure? config-m)))))

(def app (create-app (config/create-config)))

(defn -main [& args]
  (let [config-m (config/create-config)]
    (ring-jetty/run-jetty app {:port (config/port config-m)
                               :host (config/host config-m)})))

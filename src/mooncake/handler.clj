(ns mooncake.handler
  (:require [scenic.routes :as scenic]
            [ring.adapter.jetty :as ring-jetty]
            [ring.util.response :as r]
            [ring.middleware.defaults :as ring-mw]
            [mooncake.routes :as routes]
            [mooncake.config :as config]
            [mooncake.translation :as t]
            [mooncake.view.error :as error]
            [mooncake.helper :as mh]
            [mooncake.middleware :as m]))

(def default-context {:translator (t/translations-fn t/translation-map)})

(defn index [request]
  (-> (r/response "test")
      (r/content-type "text/plain")))

(defn not-found [request]
  (-> (error/not-found-error)
      (mh/enlive-response default-context)
      (r/status 404)))

(defn forbidden-err-handler [req]
  (-> (error/forbidden-error)
      (mh/enlive-response default-context)
      (r/status 403)))

(def site-handlers
  (-> {:index index}
      (m/wrap-handlers #(m/wrap-handle-403 % forbidden-err-handler) #{})
      (m/wrap-handlers #(m/wrap-handle-404 % not-found) #{})))

(defn wrap-defaults-config [secure?]
  (-> (if secure? (assoc ring-mw/secure-site-defaults :proxy true) ring-mw/site-defaults)
      (assoc-in [:session :cookie-attrs :max-age] 3600)
      (assoc-in [:session :cookie-name] "mooncake-session")))

(defn create-app [config-m]
  (-> (scenic/scenic-handler routes/routes site-handlers)
      (ring-mw/wrap-defaults (wrap-defaults-config (config/secure? config-m)))
      m/wrap-translator))

(def app (create-app (config/create-config)))

(defn -main [& args]
  (let [config-m (config/create-config)]
    (ring-jetty/run-jetty app {:port (config/port config-m)
                               :host (config/host config-m)})))

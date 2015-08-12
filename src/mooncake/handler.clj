(ns mooncake.handler
  (:require [scenic.routes :as scenic]
            [ring.adapter.jetty :as ring-jetty]
            [ring.util.response :as r]
            [ring.middleware.defaults :as ring-mw]
            [clojure.java.io :as io]
            [stonecutter-oauth.client :as soc]
            [mooncake.routes :as routes]
            [mooncake.config :as config]
            [mooncake.translation :as t]
            [mooncake.view.index :as i]
            [mooncake.view.sign-in :as si]
            [mooncake.view.error :as error]
            [mooncake.helper :as mh]
            [mooncake.activity :as a]
            [mooncake.middleware :as m])
  (:gen-class))

(defn request->config-m [request]
  (get-in request [:context :config-m]))

(def default-context {:translator (t/translations-fn t/translation-map)})

(defn index [request]
  (let [activity-sources (get-in request [:context :activity-sources])
        activities (a/retrieve-activities activity-sources)]
    (mh/enlive-response (i/index (assoc-in request [:context :activities] activities)) (:context request))))

(defn sign-in [request]
  (if (mh/signed-in? request)
    (r/redirect (routes/absolute-path (request->config-m request) :index))
    (mh/enlive-response (si/sign-in request) (:context request))))

(defn sign-out [request]
  (let [config-m (request->config-m request)]
    (-> (r/redirect (routes/absolute-path config-m :sign-in))
        (assoc :session {}))))

(defn stonecutter-sign-in [stonecutter-config request]
  (soc/authorisation-redirect-response stonecutter-config))

(defn stonecutter-callback [stonecutter-config request]
  (let [config-m (request->config-m request)
        auth-code (get-in request [:params :code])
        token-response (soc/request-access-token! stonecutter-config auth-code)
        auth-provider-user-id (:user-id token-response)]
    (-> (r/redirect (routes/absolute-path config-m :index))
        (assoc-in [:session :user-id] auth-provider-user-id))))

(defn stub-activities [request]
  (-> "stub-activities.json"
      io/resource
      slurp
      r/response
      (r/content-type "application/json")))

(defn not-found [request]
  (-> (error/not-found-error)
      (mh/enlive-response default-context)
      (r/status 404)))

(defn forbidden-err-handler [req]
  (-> (error/forbidden-error)
      (mh/enlive-response default-context)
      (r/status 403)))

(defn create-stonecutter-config [config-m]
  (soc/configure (config/auth-url config-m)
                 (config/client-id config-m)
                 (config/client-secret config-m)
                 (routes/absolute-path config-m :stonecutter-callback)))

(defn site-handlers [config-m]
  (let [stonecutter-config (create-stonecutter-config config-m)]
    (when (= :invalid-configuration stonecutter-config)
      (throw (Exception. "Invalid stonecutter configuration. Application launch aborted.")))
    (-> {:index index
         :sign-in sign-in
         :sign-out sign-out
         :stub-activities stub-activities
         :stonecutter-sign-in (partial stonecutter-sign-in stonecutter-config)
         :stonecutter-callback (partial stonecutter-callback stonecutter-config)}
        (m/wrap-handlers-excluding #(m/wrap-signed-in % (routes/absolute-path config-m :sign-in))
                                   #{:sign-in :stonecutter-sign-in :stonecutter-callback :stub-activities})
        (m/wrap-handlers-excluding #(m/wrap-handle-403 % forbidden-err-handler) #{}))))

(defn wrap-defaults-config [secure?]
  (-> (if secure? (assoc ring-mw/secure-site-defaults :proxy true) ring-mw/site-defaults)
      (assoc-in [:session :cookie-attrs :max-age] 3600)
      (assoc-in [:session :cookie-name] "mooncake-session")))

(defn create-app [config-m activity-sources]
  (-> (scenic/scenic-handler routes/routes (site-handlers config-m) not-found)
      (ring-mw/wrap-defaults (wrap-defaults-config (config/secure? config-m)))
      (m/wrap-config config-m)
      (m/wrap-activity-sources activity-sources)
      m/wrap-translator))

(defn -main [& args]
  (let [config-m (config/create-config)]
    (ring-jetty/run-jetty (create-app config-m a/activity-sources)
                          {:port (config/port config-m)
                           :host (config/host config-m)})))

(ns mooncake.handler
  (:require [scenic.routes :as scenic]
            [ring.adapter.jetty :as ring-jetty]
            [ring.util.response :as r]
            [ring.middleware.defaults :as ring-mw]
            [clojure.java.io :as io]
            [stonecutter-oauth.client :as soc]
            [mooncake.activity :as a]
            [mooncake.config :as config]
            [mooncake.db.mongo :as mongo]
            [mooncake.helper :as mh]
            [mooncake.middleware :as m]
            [mooncake.routes :as routes]
            [mooncake.translation :as t]
            [mooncake.controller.create-account :as cac]
            [mooncake.view.error :as error]
            [mooncake.view.index :as i]
            [mooncake.view.sign-in :as si]
            [mooncake.db.user :as user]
            [mooncake.schedule :as schedule])
  (:gen-class))

(def default-context {:translator (t/translations-fn t/translation-map)})

(defn index [database request]
  (let [activities (a/retrieve-activities-from-database database)]
    (mh/enlive-response (i/index (assoc-in request [:context :activities] activities)) (:context request))))

(defn get-user-id-from [request]
  (get-in request [:session :user-id]))

(defn sign-in [request]
  (if (mh/signed-in? request)
    (mh/redirect-to request :index)
    (mh/enlive-response (si/sign-in request) (:context request))))

(defn sign-out [request]
  (-> (mh/redirect-to request :sign-in)
      (assoc :session {})))

(defn stonecutter-sign-in [stonecutter-config request]
  (soc/authorisation-redirect-response stonecutter-config))

(defn stonecutter-callback [stonecutter-config db request]
  (let [auth-code (get-in request [:params :code])
        token-response (soc/request-access-token! stonecutter-config auth-code)
        auth-provider-user-id (get-in token-response [:user-info :sub])]
    (if-let [user (user/fetch-user db auth-provider-user-id)]
      (-> (mh/redirect-to request :index)
          (assoc-in [:session :username] (:username user)))
      (-> (mh/redirect-to request :show-create-account)
          (assoc-in [:session :auth-provider-user-id] auth-provider-user-id)))))

(defn stub-activities [request]
  (-> "stub-activities.json"
      io/resource
      slurp
      r/response
      (r/content-type "application/json")))

(defn internal-server-error-handler [request]
  (-> (error/internal-server-error)
      (mh/enlive-response default-context)
      (r/status 500)))

(defn not-found-handler [request]
  (-> (error/not-found-error)
      (mh/enlive-response default-context)
      (r/status 404)))

(defn forbidden-error-handler [request]
  (-> (error/forbidden-error)
      (mh/enlive-response default-context)
      (r/status 403)))

(defn create-stonecutter-config [config-m]
  (soc/configure (config/auth-url config-m)
                 (config/client-id config-m)
                 (config/client-secret config-m)
                 (routes/absolute-path config-m :stonecutter-callback)))

(defn site-handlers [config-m db]
  (let [stonecutter-config (create-stonecutter-config config-m)]
    (when (= :invalid-configuration stonecutter-config)
      (throw (Exception. "Invalid stonecutter configuration. Application launch aborted.")))
    (-> {:index                (partial index db)
         :sign-in              sign-in
         :sign-out             sign-out
         :show-create-account  cac/show-create-account
         :create-account       (partial cac/create-account db)
         :stub-activities      stub-activities
         :stonecutter-sign-in  (partial stonecutter-sign-in stonecutter-config)
         :stonecutter-callback (partial stonecutter-callback stonecutter-config db)}
        (m/wrap-handlers-excluding #(m/wrap-signed-in % (routes/absolute-path config-m :sign-in))
                                   #{:sign-in :stonecutter-sign-in :stonecutter-callback 
                                     :stub-activities :show-create-account :create-account})
        (m/wrap-handlers-excluding #(m/wrap-handle-403 % forbidden-error-handler) #{}))))

(defn wrap-defaults-config [secure?]
  (-> (if secure? (assoc ring-mw/secure-site-defaults :proxy true) ring-mw/site-defaults)
      (assoc-in [:session :cookie-attrs :max-age] 3600)
      (assoc-in [:session :cookie-name] "mooncake-session")))

(defn create-app [config-m db activity-sources]
  (a/sync-activities db activity-sources)                 ;; Ensure database is populated before starting app
  (-> (scenic/scenic-handler routes/routes (site-handlers config-m db) not-found-handler)
      (ring-mw/wrap-defaults (wrap-defaults-config (config/secure? config-m)))
      (m/wrap-config config-m)
      (m/wrap-error-handling internal-server-error-handler)
      (m/wrap-activity-sources activity-sources)
      m/wrap-translator))

(defn -main [& args]
  (let [config-m (config/create-config)
        db (mongo/create-database (mongo/get-mongo-db (config/mongo-uri config-m)))
        activity-sources a/activity-sources]
    (schedule/schedule (a/sync-activities-task db activity-sources) (config/sync-interval config-m))
    (ring-jetty/run-jetty (create-app config-m db activity-sources)
                          {:port (config/port config-m)
                           :host (config/host config-m)})))

(ns mooncake.handler
  (:require [scenic.routes :as scenic]
            [ring.adapter.jetty :as ring-jetty]
            [ring.util.response :as r]
            [ring.middleware.defaults :as ring-mw]
            [clojure.java.io :as io]
            [taoensso.tower.ring :as tower-ring]
            [stonecutter-oauth.client :as soc]
            [stonecutter-oauth.jwt :as so-jwt]
            [mooncake.activity :as a]
            [mooncake.config :as config]
            [mooncake.db.mongo :as mongo]
            [mooncake.helper :as mh]
            [mooncake.middleware :as m]
            [mooncake.routes :as routes]
            [mooncake.translation :as t]
            [mooncake.controller.feed :as fc]
            [mooncake.controller.create-account :as cac]
            [mooncake.controller.customise-feed :as cfc]
            [mooncake.view.error :as error]
            [mooncake.view.sign-in :as si]
            [mooncake.db.migration :as migration]
            [mooncake.db.user :as user]
            [mooncake.schedule :as schedule]
            [clojure.tools.logging :as log])
  (:gen-class))

(def default-context {:translator (t/translations-fn t/translation-map)})

(defn sign-in [request]
  (if (mh/signed-in? request)
    (mh/redirect-to request :feed)
    (mh/enlive-response (si/sign-in request) (:context request))))

(defn sign-out [request]
  (-> (mh/redirect-to request :sign-in)
      (assoc :session {})))

(defn stonecutter-sign-in [stonecutter-config request]
  (if-let [stub-user (config/stub-user stonecutter-config)]
    (-> (mh/redirect-to request :feed)
        (assoc-in [:session :username] stub-user))
    (soc/authorisation-redirect-response stonecutter-config)))

(defn get-auth-jwks-url [stonecutter-config]
  (str (:auth-provider-url stonecutter-config) "/api/jwk-set"))

(defn stonecutter-callback [stonecutter-config store request]
  (if-let [auth-code (get-in request [:params :code])]
    (let [token-response (soc/request-access-token! stonecutter-config auth-code)
          auth-jwks-url (get-auth-jwks-url stonecutter-config)
          public-key-string (so-jwt/get-public-key-string-from-jwk-set-url auth-jwks-url)
          user-info (so-jwt/decode stonecutter-config (:id_token token-response) public-key-string)
          auth-provider-user-id (:sub user-info)]
      (if-let [user (user/fetch-user store auth-provider-user-id)]
        (-> (mh/redirect-to request :feed)
            (assoc-in [:session :username] (:username user)))
        (-> (mh/redirect-to request :show-create-account)
            (assoc-in [:session :auth-provider-user-id] auth-provider-user-id))))
    (-> (mh/redirect-to request :sign-in)
        (assoc :flash :sign-in-failed))))

(defn stub-activities [request]
  (-> "stub-activities.json"
      io/resource
      slurp
      r/response
      (r/content-type "application/json")))

(defn internal-server-error-handler [request]
  (-> (error/internal-server-error request)
      (mh/enlive-response default-context)
      (r/status 500)))

(defn not-found-handler [request]
  (-> (error/not-found-error request)
      (mh/enlive-response default-context)
      (r/status 404)))

(defn forbidden-error-handler [request]
  (-> (error/forbidden-error request)
      (mh/enlive-response default-context)
      (r/status 403)))

(defn create-stonecutter-config [config-m]
  (soc/configure (config/auth-url config-m)
                 (config/client-id config-m)
                 (config/client-secret config-m)
                 (routes/absolute-path config-m :stonecutter-callback)
                 :protocol :openid
                 :stub-user (config/stub-user config-m)))

(defn site-handlers [config-m store activity-sources]
  (let [stonecutter-config (create-stonecutter-config config-m)]
    (when (= :invalid-configuration stonecutter-config)
      (throw (Exception. "Invalid mooncake configuration. Application launch aborted.")))
    (-> {:feed                 (partial fc/feed store)
         :sign-in              sign-in
         :sign-out             sign-out
         :show-create-account  cac/show-create-account
         :create-account       (partial cac/create-account store)
         :show-customise-feed  (partial cfc/show-customise-feed store)
         :customise-feed       (partial cfc/customise-feed store)
         :stub-activities      stub-activities
         :stonecutter-sign-in  (partial stonecutter-sign-in stonecutter-config)
         :stonecutter-callback (partial stonecutter-callback stonecutter-config store)}
        (m/wrap-handlers-excluding #(m/wrap-signed-in % (routes/absolute-path config-m :sign-in))
                                   #{:sign-in :stonecutter-sign-in :stonecutter-callback
                                     :stub-activities :show-create-account :create-account})
        (m/wrap-handlers-excluding #(m/wrap-handle-403 % forbidden-error-handler) #{})
        (m/wrap-just-these-handlers #(m/wrap-activity-sources-and-types store activity-sources %)
                                    #{:feed :show-customise-feed :customise-feed}))))

(defn wrap-defaults-config [secure?]
  (-> (if secure? (assoc ring-mw/secure-site-defaults :proxy true) ring-mw/site-defaults)
      (assoc-in [:session :cookie-attrs :max-age] 3600)
      (assoc-in [:session :cookie-name] "mooncake-session")))

(defn create-app [config-m store activity-sources]
  (a/sync-activities! store activity-sources)               ;; Ensure database is populated before starting app
  (-> (scenic/scenic-handler routes/routes (site-handlers config-m store activity-sources) not-found-handler)
      (ring-mw/wrap-defaults (wrap-defaults-config (config/secure? config-m)))
      (tower-ring/wrap-tower (t/config-translation))
      (m/wrap-config config-m)
      (m/wrap-error-handling internal-server-error-handler)
      m/wrap-translator))

(defn -main [& args]
  (let [config-m (config/create-config)
        mongo-db (mongo/get-mongo-db (config/mongo-uri config-m))
        store (mongo/create-mongo-store mongo-db)
        activity-sources a/activity-sources]
    (if-let [stub-user (config/stub-user config-m)]
      (when-not (user/find-user store stub-user)
        (user/create-user! store nil stub-user)))
    (migration/run-migrations mongo-db)
    (schedule/schedule (a/sync-activities-task store activity-sources) (config/sync-interval config-m))
    (ring-jetty/run-jetty (create-app config-m store activity-sources)
                          {:port (config/port config-m)
                           :host (config/host config-m)})))

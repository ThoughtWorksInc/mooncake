(ns mooncake.handler
  (:require [scenic.routes :as scenic]
            [ring.adapter.jetty :as ring-jetty]
            [ring.util.response :as r]
            [ring.middleware.defaults :as ring-mw]
            [clj-http.client :as http]
            [clj-yaml.core :as yaml]
            [clojure.java.io :as io]
            [clojure.tools.logging :as log]
            [mooncake.routes :as routes]
            [mooncake.config :as config]
            [mooncake.translation :as t]
            [mooncake.view.index :as i]
            [mooncake.view.error :as error]
            [mooncake.helper :as mh]
            [mooncake.middleware :as m]))

(def default-context {:translator (t/translations-fn t/translation-map)})

(defn load-activity-sources [activity-resource-name]
  (-> activity-resource-name
      io/resource
      slurp
      yaml/parse-string))

(def activity-sources
  (load-activity-sources "activity-sources.yml"))

(defn get-json-from-activity-source [url]
  (try
    (:body (http/get url {:accept :json :as :json-string-keys}))
    (catch Exception e
      (log/warn (str "Unable to retrieve activities from " url " --- " e))
      nil)))

(defn retrieve-activities-from-source [source-k-v-pair]
  (let [[source-key source-url] source-k-v-pair
        activities (get-json-from-url source-url)]
    (map #(assoc % :activity-src source-key) activities)))

(defn retrieve-activities [activity-sources]
  (let [published-time (fn [activity]
                         (mh/datetime-str->datetime (get activity "published")))]
    (->> (map retrieve-activities-from-source activity-sources)
         flatten
         (sort-by published-time mh/after?))))

(defn index [request]
  (let [activity-sources (get-in request [:context :activity-sources])
        activities (retrieve-activities activity-sources)]
    (mh/enlive-response (i/index (assoc-in request [:context :activities] activities)) default-context)))

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

(def site-handlers
  (-> {:index index
       :stub-activities stub-activities}
      (m/wrap-handlers #(m/wrap-handle-403 % forbidden-err-handler) #{})))

(defn wrap-defaults-config [secure?]
  (-> (if secure? (assoc ring-mw/secure-site-defaults :proxy true) ring-mw/site-defaults)
      (assoc-in [:session :cookie-attrs :max-age] 3600)
      (assoc-in [:session :cookie-name] "mooncake-session")))

(defn create-app [config-m activity-sources]
  (-> (scenic/scenic-handler routes/routes site-handlers not-found)
      (ring-mw/wrap-defaults (wrap-defaults-config (config/secure? config-m)))
      (m/wrap-config config-m)
      (m/wrap-activity-sources activity-sources)
      m/wrap-translator))

(def app (create-app (config/create-config) activity-sources))

(defn -main [& args]
  (let [config-m (config/create-config)]
    (ring-jetty/run-jetty app {:port (config/port config-m)
                               :host (config/host config-m)})))

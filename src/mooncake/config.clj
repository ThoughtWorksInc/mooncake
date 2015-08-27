(ns mooncake.config
  (:require [environ.core :as env]
            [clojure.tools.logging :as log]))

(def env-vars #{:port :host :base-url
                :mongo-uri :mongo-port-27017-tcp-addr
                :client-id :client-secret :auth-url
                :secure :sync-interval})

(defn create-config []
  (select-keys env/env env-vars))

(defn get-env
  "Like a normal 'get' except it also ensures the key is in the env-vars set"
  ([config-m key]
   (get-env config-m key nil))
  ([config-m key default]
   (when-not (env-vars key)
     (throw (Exception. (format "Trying to get-env with key '%s' which is not in the env-vars set" key))))
   (get config-m (env-vars key) default)))

(defn port [config-m]
  (Integer. (get-env config-m :port "3000")))

(defn host [config-m]
  (get-env config-m :host "localhost"))

(defn base-url [config-m]
  (get-env config-m :base-url "http://localhost:3000"))

(defn client-id [config-m]
  (get-env config-m :client-id))

(defn client-secret [config-m]
  (get-env config-m :client-secret))

(defn auth-url [config-m]
  (get-env config-m :auth-url))

(defn sync-interval [config-m]
  (Integer. (get-env config-m :sync-interval 60)))

(defn secure?
  "Returns true unless 'secure' environment variable set to 'false'"
  [config-m]
  (not (= "false" (get-env config-m :secure "true"))))

(defn- get-docker-mongo-uri [config-m]
  (when-let [mongo-ip (get-env config-m :mongo-port-27017-tcp-addr)]
    (format "mongodb://%s:27017/stonecutter" mongo-ip)))

(defn mongo-uri [config-m]
  (or
    (get-docker-mongo-uri config-m)
    (get-env config-m :mongo-uri)
    "mongodb://localhost:27017/mooncake"))

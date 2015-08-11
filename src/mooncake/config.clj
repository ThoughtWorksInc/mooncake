(ns mooncake.config
  (:require [environ.core :as env]
            [clojure.tools.logging :as log]))

(def env-vars #{:port :host :secure})

(defn create-config []
  (select-keys env/env env-vars))

(defn get-env
  "Like a normal 'get' except it also ensures the key is in the env-vars set"
  ([config-m key]
   (get config-m (env-vars key)))
  ([config-m key default]
   (get config-m (env-vars key) default)))

(defn port [config-m]
  (Integer. (get-env config-m :port "3000")))

(defn host [config-m]
  (get-env config-m :host "127.0.0.1"))

(defn scheme [config-m]
  (get-env config-m :scheme "https"))

(defn base-url [config-m]
  (str (scheme config-m) "://" (host config-m) ":" (port config-m)))

(defn client-id [config-m]
  (get-env config-m :client-id))

(defn client-secret [config-m]
  (get-env config-m :client-secret))

(defn auth-url [config-m]
  (get-env config-m :auth-url))

(defn secure?
  "Returns true unless 'secure' environment variable set to 'false'"
  [config-m]
  (not (= "false" (get-env config-m :secure "true"))))


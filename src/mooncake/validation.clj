(ns mooncake.validation
  (:require [clojure.string :as s]))

(def username-max-length 16)

(defn is-too-long? [string max-length]
  (> (count string) max-length))

(defn is-format-valid? [username]
  (when username
    (re-matches #"[a-zA-Z0-9_]+" username)))

(defn validate-username [username duplicate-username-fn]
  (when-let [validation-issue
             (cond
               (s/blank? username)                         :blank
               (is-too-long? username username-max-length) :too-long
               (not (is-format-valid? username))           :invalid-format
               (duplicate-username-fn username)            :duplicate)]
    {:username validation-issue}))

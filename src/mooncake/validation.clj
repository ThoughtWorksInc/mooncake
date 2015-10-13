(ns mooncake.validation
  (:require [clojure.string :as s]
            [clj-time.coerce :as time-coerce]))

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


(defn is-published-format-valid? [timestamp]
  (time-coerce/from-string timestamp))

(defn validate-published [activity]
  (if-not (clojure.string/blank? (:published activity))
    (when-not (is-published-format-valid? (:published activity))
      :invalid)
    :blank))

(defn return-nil-if-empty [c]
  (when-not (empty? c) c))

(defn return-errors-or-nil [error-m]
  (->>
    error-m
    (remove (fn [[k v]] (nil? v)))
    (into {})
    return-nil-if-empty))

(defn validate-activity [activity]
  (->>
    {:published (validate-published activity)}
    return-errors-or-nil))

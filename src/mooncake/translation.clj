(ns mooncake.translation
  (:require [clj-yaml.core :as yaml]
            [clojure.java.io :as io]
            [clojure.tools.logging :as log]
            [traduki.core :as t]))

(defn load-translations-from-string [s]
  (yaml/parse-string s))

(defn translation-map [file-name]
  (-> file-name
      io/resource
      slurp
      load-translations-from-string))

(defn config-translation []
  {:dictionary                 {:en (translation-map "en.yml")
                                :fi (translation-map "fi.yml")}
   :dev-mode?                  false
   :fallback-locale            :en
   :log-missing-translation-fn (fn [{:keys [locales ks ns]}]
                                 (log/warn (str "Missing translation! locales: " locales
                                                ", keys: " ks ", namespace: " ns)))})

(defn get-locale-from-request [request]
  (if-let [session-locale (get-in request [:session :locale])]
    session-locale
    (get request :locale :en)))

(defn context-translate [enlive-m request]
  (let [locale (get-locale-from-request request)]
    (t/translate (partial (:t request) locale) enlive-m)))

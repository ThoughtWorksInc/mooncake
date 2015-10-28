(ns mooncake.translation
  (:require [clj-yaml.core :as yaml]
            [clojure.java.io :as io]
            [clojure.tools.logging :as log]
            [traduki.core :as t]
            [taoensso.tower :as tower]))

(defn load-translations-from-string [s]
  (yaml/parse-string s))

(defn load-translations-from-file [file-name]
  (-> file-name
      io/resource
      slurp
      load-translations-from-string))

(defn translation-map [file-name]
  (load-translations-from-file file-name))

(defn translations-fn [translation-map]
  (fn [translation-key]
    (let [key1 (keyword (namespace translation-key))
          key2 (keyword (name translation-key))
          translation (get-in translation-map [key1 key2])]
      (when-not translation (log/warn (str "No translation found for " translation-key)))
      translation)))

(defn config-translation []
  {:dictionary                 {:en (translation-map "en.yml")
                                :fi (translation-map "fi.yml")}
   :dev-mode?                  false
   :fallback-locale            :en
   :log-missing-translation-fn (fn [{:keys [locales ks ns] :as args}]
                                 (log/warn (str "Missing translation! locales: " locales
                                                ", keys: " ks ", namespace: " ns)))})

(defn get-locale-from-request [request]
  (if-let [session-locale (get-in request [:session :locale])]
    session-locale
    (get request :locale :en)))

(defn context-translate [enlive-m request]
  (let [locale (get-locale-from-request request)]
    (t/translate (partial (:t request) locale) enlive-m)))

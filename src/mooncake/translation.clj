(ns mooncake.translation
  (:require [clj-yaml.core :as yaml]
            [clojure.java.io :as io]
            [clojure.tools.logging :as log]
            [traduki.core :as t]))

(defn load-translations-from-string [s]
  (yaml/parse-string s))

(defn load-translations-from-file [file-name]
  (-> file-name
      io/resource
      slurp
      load-translations-from-string))

(defn deep-merge
  "Recursively merges maps. If keys are not maps, the last value wins."
  [& vals]
  (if (every? map? vals)
    (apply merge-with deep-merge vals)
    (last vals)))

(defn translation-map [lang]
  (load-translations-from-file (str "lang/" lang ".yml")))

(defn config-translation []
  {:dictionary                 {:en (translation-map "en")
                                :fi (translation-map "fi")}
   :dev-mode?                  false
   :fallback-locale            :en
   :log-missing-translation-fn (fn [{:keys [locales ks ns]}]
                                 (log/warn (str "Missing translation! locales: " locales
                                                ", keys: " ks ", namespace: " ns)))})

(defn get-locale-from-request [request]
  (if-let [session-locale (get-in request [:session :locale])]
    session-locale
    (get request :locale :en)))

(defn has-locale [locale]
  ((keyword locale) (:dictionary (config-translation))))

(defn context-translate [enlive-m request]
  (let [locale (get-locale-from-request request)]
    (t/translate (partial (:t request) locale) enlive-m)))

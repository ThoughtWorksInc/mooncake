(ns mooncake.view.view-helpers
  (:require [ring.util.anti-forgery :refer [anti-forgery-field]]
            [net.cgrand.enlive-html :as html]
            [net.cgrand.jsoup :as jsoup]
            [clojure.tools.logging :as log]
            [mooncake.routes :as routes]
            [mooncake.translation :as translation]
            [clojure.string :as string]))

(defn remove-element [enlive-m selector]
  (html/at enlive-m selector nil))

(defn anti-forgery-snippet []
  (html/html-snippet (anti-forgery-field)))

(defn add-anti-forgery [enlive-m]
  (html/at enlive-m
           [:form] (html/prepend (anti-forgery-snippet))))

(defn add-logo-link [enlive-m]
  (html/at enlive-m [:.clj--header__logo :a] (html/set-attr :href (routes/path :feed))))

(def template-caching? (atom true))

(def template-cache (atom {}))

(defn update-language [enlive-m lang]
  (let [language-key (or (when (translation/has-locale lang) lang) :en)]
    (html/at enlive-m [:html] (html/set-attr :lang (name language-key)))))

(defn html-resource-with-log [path]
  (log/debug (format "Loading template %s from file" path))
  (html/html-resource path {:parser jsoup/parser}))

(defn load-template [path]
  (if @template-caching?
    (if (contains? @template-cache path)
      (get @template-cache path)
      (let [html (html-resource-with-log path)]
        (swap! template-cache #(assoc % path html))
        html))
    (html-resource-with-log path)))

(defn load-template-with-lang [path lang]
  (update-language (load-template path) lang))

(defn enlive-to-str [nodes]
  (->> nodes
       html/emit*
       (apply str)))

(defn remove-elements [enlive-m selector]
  (html/at enlive-m selector nil))

(def whitespace-or-punctuation-character-regex "[\\s[\\p{Punct}&&[^\\-]]]")
(def word-break-character-or-end-of-line (str "(" whitespace-or-punctuation-character-regex "|$)"))

(defn- create-max-characters-matching-pattern
  "Creates a pattern for matching at most n characters, matching whole words: ^(.{0,15}(-|[^\\s\\p{Punct}]))([\\s[\\p{Punct}&&[^\\-]]]|$)"
  [max-char-count]
  (let [any-character-other-then-punctuation-or-whitespace "(-|[^\\s\\p{Punct}])"
        any-character-at-most-n-times  (str ".{0," (- max-char-count 1) "}")
        group-of-characters-at-most-n-times (str "(" any-character-at-most-n-times any-character-other-then-punctuation-or-whitespace ")")]
    (re-pattern (str "(?s)^" group-of-characters-at-most-n-times word-break-character-or-end-of-line))))

(defn limit-characters [max-char-count text]
  (when-not (nil? text)
    (let [search-pattern (create-max-characters-matching-pattern max-char-count)
          regex-match (re-find search-pattern text)]
      (if (nil? regex-match)
        (subs text 0 (min (count text) max-char-count))
        (second regex-match)))))

(defn limit-text-length-if-above [max-char-count text]
  (if (> (count text) max-char-count)
    (str (limit-characters max-char-count text) "\u2026")
    text))

(defn add-script [enlive-m script-path]
  (let [script-tag (html/as-nodes {:tag :script :attrs {:src script-path}})]
    (html/at enlive-m [:body] (html/append script-tag))))

(defn format-translation-str [s]
  (-> s
      (string/replace #" " "-")
      string/lower-case))
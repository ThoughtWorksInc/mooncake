(ns mooncake.test.test-helpers.enlive
  (:require [midje.sweet :as midje]
            [net.cgrand.enlive-html :as html]
            [mooncake.helper :as mh]
            [mooncake.translation :as t]
            [mooncake.routes :as routes]
            [clojure.tools.logging :as log]))

(defn check-redirects-to [path]
  (midje/checker [response] (and
                              (= (:status response) 302)
                              (= (get-in response [:headers "Location"]) path))))

(defn check-renders-page [body-class-enlive-selector]
  (midje/checker [response] (and
                              (= (:status response) 200)
                              (not-empty (-> (html/html-snippet (:body response))
                                             (html/select [body-class-enlive-selector]))))))

(def no-untranslated-strings
  (let [untranslated-string-regex #"(?!!DOCTYPE|!IEMobile)!\w+"]
    (midje/chatty-checker [response-body] (empty? (re-seq untranslated-string-regex response-body)))))

(defn translations-fn [translation-map]
  (fn [translation-key]
    (let [key1 (keyword (namespace translation-key))
          key2 (keyword (name translation-key))
          translation (get-in translation-map [key1 key2])]
      (when-not translation (log/warn (str "No translation found for " translation-key)))
      translation)))

(defn test-translations
  ([page-name view-fn]
   (test-translations page-name view-fn {} {}))
  ([page-name view-fn context]
   (test-translations page-name view-fn context {}))
  ([page-name view-fn context request]
   (midje/fact {:midje/name (format "Checking all translations exist for %s" page-name)}
               (let [translator (translations-fn t/translation-map)
                     page (-> (assoc request :context (assoc context :translator translator))
                              view-fn
                              (mh/enlive-response {:t {}})
                              :body)]
                 page => no-untranslated-strings))))

(defn enlive-m->attr [enlive-m selector attr]
  (-> enlive-m (html/select selector) first :attrs attr))

(defn enlive-m->text [enlive-m selector]
  (-> enlive-m (html/select selector) first html/text))

(defn text-is? [selector text]
  (midje/chatty-checker [enlive-m]
                        (= text (enlive-m->text enlive-m selector))))

(defn has-attr? [selector attr attr-val]
  (midje/chatty-checker [enlive-m]
                        (= attr-val (enlive-m->attr enlive-m selector attr))))

(defn has-form-action?
  ([path]
   (has-form-action? [:form] path))

  ([form-selector path]
   (has-attr? form-selector :action path)))

(defn has-form-method?
  ([method]
   (has-form-method? [:form] method))

  ([form-selector method]
   (has-attr? form-selector :method method)))

(defn links-to? [selector path]
  (has-attr? selector :href path))

(defn has-class? [selector css-class]
  (fn [enlive-m]
    ((midje/contains css-class) (enlive-m->attr enlive-m selector :class))))

(defn test-logo-link [view-fn]
  (midje/fact {:midje/name "Checking logo has a correct link"}
              (let [page (-> (view-fn ...request...)
                             (mh/enlive-response {:t {}})
                             :body
                             (html/html-snippet))]
                page => (links-to? [:.clj--header__logo :a] (routes/path :feed)))))

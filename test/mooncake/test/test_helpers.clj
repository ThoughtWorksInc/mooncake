(ns mooncake.test.test-helpers
  (:require [midje.sweet :as midje]
            [net.cgrand.enlive-html :as html]
            [mooncake.helper :as h]
            [mooncake.translation :as t]))

(defn check-redirects-to [path]
  (midje/checker [response] (and
                              (= (:status response) 302)
                              (= (get-in response [:headers "Location"]) path))))

(def no-untranslated-strings
  (let [untranslated-string-regex #"(?!!DOCTYPE|!IEMobile)!\w+"]
    (midje/chatty-checker [response-body] (empty? (re-seq untranslated-string-regex response-body)))))

(defn test-translations [page-name view-fn]
  (midje/fact {:midje/name (format "there are no missing translations for page: %s" page-name)}
              (let [translator (t/translations-fn t/translation-map)
                    page (-> {:context {:translator translator}}
                             view-fn
                             (h/enlive-response {:translator translator}) :body)]
                page => no-untranslated-strings)))

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

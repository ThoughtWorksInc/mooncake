(ns mooncake.test.test-helpers
  (:require [midje.sweet :as midje]
            [net.cgrand.enlive-html :as html]))

(defn check-redirects-to [path]
  (midje/checker [response] (and
                        (= (:status response) 302)
                        (= (get-in response [:headers "Location"]) path))))


(defn enlive-m->attr [enlive-m selector attr]
  (-> enlive-m (html/select selector) first :attrs attr))

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

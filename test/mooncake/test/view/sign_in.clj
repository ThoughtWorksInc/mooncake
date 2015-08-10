(ns mooncake.test.view.sign-in
  (:require [midje.sweet :refer :all]
            [net.cgrand.enlive-html :as html]
            [mooncake.test.test-helpers :as th]
            [mooncake.view.sign-in :as si]))

(fact "sign-in page should render sign-in template"
      (let [page (si/sign-in ...request...)]
        page => (th/has-attr? [:body] :class "func--sign-in-page")))

(fact "form has correct action and method"
      (let [page (si/sign-in ...request...)]
        page => (th/has-form-action? "d-cent-sign-in")
        page => (th/has-form-method? "get")))

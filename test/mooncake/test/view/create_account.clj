(ns mooncake.test.view.create-account
  (:require [midje.sweet :refer :all]
            [mooncake.test.test-helpers :as th]
            [mooncake.routes :as r]
            [mooncake.view.create-account :as ca]))

(fact "create-account page should render create-account template"
      (let [page (ca/create-account ...request...)]
        page => (th/has-class? [:body] "func--create-account-page")))

(th/test-translations "create-account page" ca/create-account)

(fact "create account form action is set correctly"
      (let [page (ca/create-account ...request...)]
        page => (every-checker
                  (th/has-form-method? "post")
                  (th/has-form-action? (r/path :create-account)))))

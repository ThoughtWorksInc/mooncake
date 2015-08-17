(ns mooncake.test.view.create-account
  (:require [midje.sweet :refer :all]
            [mooncake.test.test-helpers :as th]
            [mooncake.view.create-account :as ca]))

(fact "create-account page should render create-account template"
      (let [page (ca/create-account ...request...)]
        page => (th/has-class? [:body] "func--create-account-page")))

;(th/test-translations "Create-account page" ca/create-account)

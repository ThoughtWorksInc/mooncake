(ns mooncake.test.view.sign-in
  (:require [midje.sweet :refer :all]
            [net.cgrand.enlive-html :as html]
            [mooncake.test.test-helpers :as th]
            [mooncake.view.sign-in :as si]))

(fact "sign-in page should render sign-in template"
      (let [page (si/sign-in ...request...)]
        page => (th/has-attr? [:body] :class "func--sign-in-page")))

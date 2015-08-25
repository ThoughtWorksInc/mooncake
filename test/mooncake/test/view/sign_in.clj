(ns mooncake.test.view.sign-in
  (:require [midje.sweet :refer :all]
            [mooncake.routes :as routes]
            [mooncake.test.test-helpers.enlive :as eh]
            [mooncake.view.sign-in :as si]))

(fact "sign-in page should render sign-in template"
      (let [page (si/sign-in ...request...)]
        page => (eh/has-class? [:body] "func--sign-in-page")))

(eh/test-translations "Sign-in page" si/sign-in)

(fact "sign-in links to the correct location"
      (let [page (si/sign-in ...request...)]
        page => (eh/links-to? [:.clj--sign-in-with-d-cent] (routes/path :stonecutter-sign-in))))

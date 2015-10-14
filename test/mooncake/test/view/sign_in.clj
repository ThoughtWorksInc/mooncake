(ns mooncake.test.view.sign-in
  (:require [midje.sweet :refer :all]
            [mooncake.routes :as routes]
            [mooncake.test.test-helpers.enlive :as eh]
            [mooncake.view.sign-in :as si]
            [net.cgrand.enlive-html :as html]))

(fact "sign-in page should render sign-in template"
      (let [page (si/sign-in ...request...)]
        page => (eh/has-class? [:body] "func--sign-in-page")))

(eh/test-translations "Sign-in page" si/sign-in)
(eh/test-logo-link si/sign-in)

(fact "sign-in links to the correct location"
      (let [page (si/sign-in ...request...)]
        page => (eh/links-to? [:.clj--sign-in-with-d-cent] (routes/path :stonecutter-sign-in))))

(facts "about a flash message"
       (fact "no flash messages are displayed by default"
             (let [page (si/sign-in {:flash {}})]
               (-> page (html/select [:.clj--flash-message-container])) => empty?))

       (fact "sign-in-failed flash message is displayed on the page if it is in the flash of request"
             (let [page (si/sign-in {:flash {:sign-in-failed true}})]
               (-> page (html/select [:.clj--flash-message-container])) =not=> empty?)))

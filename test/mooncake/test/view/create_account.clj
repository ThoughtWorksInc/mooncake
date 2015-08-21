(ns mooncake.test.view.create-account
  (:require [midje.sweet :refer :all]
            [net.cgrand.enlive-html :as html]
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

(fact "about removing the username validation element when there are no validation errors"
      (let [page (ca/create-account ...request...)]
        (html/select page [:.form-row__validation]) => empty?
        (html/select page [:.clj--username__validation]) => empty?))

(tabular
  (fact "username validation errors are displayed"
        (let [error-m {:username ?error-key}
              params {:username ?username}
              page (ca/create-account {:context {:params params :error-m error-m}})]
          (fact "the class for styling errors is added"
                (html/select page [[:.clj--username :.form-row--validation-error]]) =not=> empty?)
          (fact "username validation element is present"
                (html/select page [:.clj--username__validation]) =not=> empty?)
          (fact "input is retained"
                (first (html/select page [:.clj--username__input])) =>
                (contains {:attrs (contains {:value ?username})}))
          (fact "correct error message is displayed"
                (first (html/select page [:.clj--username__validation]))
                => (contains {:attrs (contains {:data-l8n ?validation-translation})}))))

  ?error-key        ?username                     ?validation-translation
  :blank            "  "                          "content:create-account/username-blank-validation-message"
  :too-long         "this_username_is_too_long"   "content:create-account/username-too-long-validation-message"
  :invalid-format   "invalid@-!*"                 "content:create-account/username-invalid-format-validation-message"
  :duplicate        "duplicate_user"              "content:create-account/username-duplicate-validation-message"
  :some-other-one   "some_username"               "content:create-account/username-default-validation-message")

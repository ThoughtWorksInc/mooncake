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

(facts "about displaying validation errors"
       (facts "when username is blank"
              (let [error-m {:username :blank}
                    params {:username ""}
                    page (ca/create-account {:params params
                                             :context {:error-m error-m}})]
                (fact "the class for styling errors is added"
                      (html/select page [[:.clj--username :.form-row--validation-error]]) =not=> empty?)
                (fact "username validation element is present"
                      (html/select page [:.clj--username__validation]) =not=> empty?)
                (fact "correct error message is displayed"
                      (html/select page [[:.clj--username__validation (html/attr= :data-l8n "content:create-account/username-blank-validation-message")]]) =not=> empty?))))

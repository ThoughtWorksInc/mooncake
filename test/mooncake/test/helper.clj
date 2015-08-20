(ns mooncake.test.helper
  (:require [midje.sweet :refer :all]
            [net.cgrand.enlive-html :as html]
            [mooncake.helper :as mh]))

(fact "enlive response removes elements with clj--STRIP"
      (let [some-html "<p>kept</p>
                      <h1 class=\"clj--STRIP\">stripped</h1>
                      <div class=\"clj--STRIP\">stripped</div>"
            enlive-m (html/html-snippet some-html)
            response-body (:body (mh/enlive-response enlive-m {:translator {}}))]
        response-body => (contains "kept")
        response-body =not=> (contains "stripped")))

(fact "CSRF token is added to forms"
      (let [some-html "<form></form><form></form>"
            enlive-m (html/html-snippet some-html)
            response-body (:body (mh/enlive-response enlive-m {:translator {}}))]
        (-> (html/html-snippet response-body)
            (html/select [:input])) => (two-of (contains {:attrs (contains {:name "__anti-forgery-token"})}))))


(tabular 
  (fact "about authenticated?"
        (mh/authenticated? ?request) => ?result)
  ?request                                          ?result
  {}                                                false
  {:session nil}                                    false
  {:session {:username ...username...}}             false
  {:session {:auth-provider-user-id nil}}           false
  {:session {:auth-provider-user-id ...user-id...}} true)

(tabular 
  (fact "about signed-in?"
        (mh/signed-in? ?request) => ?result)
  ?request                                          ?result
  {}                                                false
  {:session nil}                                    false
  {:session {:auth-provider-user-id ...user-id...}} false
  {:session {:username nil}}                        false
  {:session {:username ...username...}}             true)

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

(tabular 
  (fact "about authenticated?"
        (mh/authenticated? ?request) => ?result)
  ?request                                          ?result
  {}                                                false
  {:session nil}                                    false
  {:session {:user-name ...user-name...}}           false
  {:session {:auth-provider-user-id nil}}           false
  {:session {:auth-provider-user-id ...user-id...}} true)

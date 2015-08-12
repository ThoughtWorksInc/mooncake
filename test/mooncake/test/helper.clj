(ns mooncake.test.helper
  (:require [midje.sweet :refer :all]
            [net.cgrand.enlive-html :as html]
            [mooncake.helper :as h]))

(fact "enlive response removes elements with clj--STRIP"
      (let [some-html "<p>kept</p>
                      <h1 class=\"clj--STRIP\">stripped</h1>
                      <div class=\"clj--STRIP\">stripped</div>"
            enlive-m (html/html-snippet some-html)
            response-body (:body (h/enlive-response enlive-m {}))]
        response-body => (contains "kept")
        response-body =not=> (contains "stripped")))

(ns mooncake.test.helper
  (:require [midje.sweet :refer :all]
            [clj-time.format :as f]
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


(tabular
  (fact "about datetime-str->datetime with time zones and optional milliseconds"
        (f/unparse (f/formatters :date-time) (mh/datetime-str->datetime ?datetime-str-in)) => ?datetime-str-out)

  ?datetime-str-in                            ?datetime-str-out
  "2015-09-06T11:05:53+03:00"                 "2015-09-06T08:05:53.000Z"
  "2015-09-06T12:05:53Z"                      "2015-09-06T12:05:53.000Z"
  "2015-09-06T13:05:53.213Z"                  "2015-09-06T13:05:53.213Z"
  "2015-09-06T14:05:53.542-02:00"             "2015-09-06T16:05:53.542Z")

(fact "about has-keys?"
      (let [map {:a "bob" :b "barry" :c "joshua"}
            contained-keys [:a :b]
            not-contained-keys [:a :d]]
        (mh/has-keys? map contained-keys) => true
        (mh/has-keys? map not-contained-keys) => false))

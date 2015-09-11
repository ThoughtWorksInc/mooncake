(ns mooncake.test.view.feed
  (:require [midje.sweet :refer :all]
            [net.cgrand.enlive-html :as html]
            [clj-time.core :as c]
            [clj-time.format :as f]
            [mooncake.routes :as routes]
            [mooncake.test.test-helpers.enlive :as eh]
            [mooncake.view.feed :as fv]))

(fact "index page should return index template"
      (let [page (fv/feed :request)]
        page => (eh/has-class? [:body] "func--index-page")))

(eh/test-translations "Index page" fv/feed)
(eh/test-logo-link fv/feed)

(fact "username is rendered"
      (fv/feed {:session {:username "Dave"}}) => (eh/text-is? [:.clj--username] "Dave"))

(fact "sign-out link is rendered and directs to /sign-out when user is signed in"
      (let [page (fv/feed {:session {:username ...username...}})]
        page => (eh/links-to? [:.clj--sign-out__link] (routes/path :sign-out))
        page =not=> (eh/has-class? [:.clj--sign-out__link] "clj--STRIP")))

(fact "activities are rendered on the page"
      (let [ten-minutes-ago (-> -10 c/minutes c/from-now)
            ten-minutes-ago-str (f/unparse (f/formatters :date-time) ten-minutes-ago)
            page (fv/feed {:context {:activities
                                     [{"activity-src" "an-objective8-activity-src"
                                       "@context"     "http://www.w3.org/ns/activitystreams"
                                       "@type"        "Create"
                                       "published"    ten-minutes-ago-str
                                       "actor"        {"@type"       "Person"
                                                       "displayName" "JDog"}
                                       "object"       {"@type"       "Objective"
                                                       "displayName" "OBJECTIVE 7 TITLE"
                                                       "content"     "We want to establish Activity Types for Objective8"
                                                       "url"         "http://objective8.dcentproject.eu/objectives/7"}}
                                      {"activity-src" "a-helsinki-activity-src"
                                       "@context"     "http://www.w3.org/ns/activitystreams"
                                       "@type"        "Add"
                                       "published"    "2015-09-06T11:05:53+03:00"
                                       "actor"        {"@type"       "Group"
                                                       "displayName" "Kaupunginjohtaja/J"
                                                       "@id"         "http://dev.hel.fi/paatokset/v1/policymaker/50/"}
                                       "object"       {"@id"         "http://dev.hel.fi/paatokset/v1/agenda_item/52359/"
                                                       "displayName" "Ymp\u00e4rist\u00f6raportoinnin asiantuntijaty\u00f6ryhm\u00e4n asettaminen toimikaudeksi 2015\u20132020"
                                                       "@type"       "Content"
                                                       "url"         "http://dev.hel.fi/paatokset/asia/hel-2015-005343/11010vh1j-2015-25/"
                                                       "content"     "some Finnish HTML"}}
                                      {"activity-src" "another-objective8-activity-src"
                                       "@context"     "http://www.w3.org/ns/activitystreams"
                                       "@type"        "Create"
                                       "published"    "2015-08-04T14:49:38.407Z"
                                       "actor"        {"@type"       "Person"
                                                       "displayName" "Lala"}
                                       "object"       {"@type"       "Objective"
                                                       "displayName" "OBJECTIVE 6 TITLE"
                                                       "description" "Yes."
                                                       "url"         "http://objective8.dcentproject.eu/objectives/6"}}]}})
            [first-activity-item second-activity-item third-activity-item] (html/select page [:.clj--activity-item])]

        (count (html/select page [:.clj--activity-item])) => 3

        first-activity-item => (eh/links-to? [:.clj--activity-item__link] "http://objective8.dcentproject.eu/objectives/7")
        first-activity-item => (eh/has-attr? [:.clj--activity-item__time] :datetime ten-minutes-ago-str)
        first-activity-item => (eh/text-is? [:.clj--activity-item__time] "10 minutes ago")
        first-activity-item => (eh/text-is? [:.clj--activity-item__action] "JDog - Objective - Create")
        first-activity-item => (eh/text-is? [:.clj--activity-item__title] "OBJECTIVE 7 TITLE")

        second-activity-item => (eh/links-to? [:.clj--activity-item__link] "http://dev.hel.fi/paatokset/asia/hel-2015-005343/11010vh1j-2015-25/")
        second-activity-item => (eh/has-attr? [:.clj--activity-item__time] :datetime "2015-09-06T11:05:53+03:00")
        second-activity-item => (eh/text-is? [:.clj--activity-item__action] "Kaupunginjohtaja/J - Content - Add")
        second-activity-item => (eh/text-is? [:.clj--activity-item__title] "Ymp\u00e4rist\u00f6raportoinnin asiantuntijaty\u00f6ryhm\u00e4n asettaminen toimikaudeksi 2015\u20132020")

        third-activity-item => (eh/links-to? [:.clj--activity-item__link] "http://objective8.dcentproject.eu/objectives/6")
        third-activity-item => (eh/has-attr? [:.clj--activity-item__time] :datetime "2015-08-04T14:49:38.407Z")
        third-activity-item => (eh/text-is? [:.clj--activity-item__action] "Lala - Objective - Create")
        third-activity-item => (eh/text-is? [:.clj--activity-item__title] "OBJECTIVE 6 TITLE")))

(fact "activity item avatars are given the initial of the actor (the name of the person)"
      (let [page (fv/feed {:context {:activities
                                     [{"activity-src" "an-activity-src"
                                       "actor"        {"@type"       "Person"
                                                       "displayName" "abby"}}
                                      {"activity-src" "an-activity-src"
                                       "actor"        {"@type"       "Person"
                                                       "displayName" "Bobby"}}
                                      {"activity-src" "an-activity-src"
                                       "actor"        {"@type"       "Person"
                                                       "displayName" "2k12carlos"}}]}})
            initials-elements (-> (html/select page [:.clj--avatar__initials]))]
        (html/text (first initials-elements)) => "A"
        (html/text (second initials-elements)) => "B"
        (html/text (nth initials-elements 2 nil)) => "2"))

(fact "activity item avatars are assigned the correct classes so they can be colour-coded by activity source"
      (let [page (fv/feed {:context {:activities
                                     [{"activity-src" "an-activity-src"}
                                      {"activity-src" "another-activity-src"}
                                      {"activity-src" "an-activity-src"}]}})
            first-activity-item-class (-> (html/select page [:.clj--activity-item])
                                          first :attrs :class)
            second-activity-item-class (-> (html/select page [:.clj--activity-item])
                                           second :attrs :class)
            third-activity-item-class (-> (html/select page [:.clj--activity-item])
                                          (nth 2 nil) :attrs :class)]
        first-activity-item-class => (contains "activity-src-0")
        first-activity-item-class =not=> (contains "activity-src-1")
        (count (re-seq #"activity-src-" first-activity-item-class)) => 1
        second-activity-item-class => (contains "activity-src-1")
        second-activity-item-class =not=> (contains "activity-src-0")
        (count (re-seq #"activity-src-" second-activity-item-class)) => 1
        third-activity-item-class => (contains "activity-src-0")
        third-activity-item-class =not=> (contains "activity-src-1")
        (count (re-seq #"activity-src-" third-activity-item-class)) => 1))

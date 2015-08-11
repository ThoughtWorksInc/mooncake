(ns mooncake.test.view.index
  (:require [midje.sweet :refer :all]
            [net.cgrand.enlive-html :as html]
            [clojure.contrib.humanize :as h]
            [clj-time.core :as c]
            [clj-time.format :as f]
            [mooncake.test.test-helpers :as th]
            [mooncake.view.index :as i]))

(fact "index page should return index template"
      (let [page (i/index :request)]
        page => (th/has-attr? [:body] :class "func--index-page")))

(th/test-translations "Index page" i/index)

(fact "activities are rendered on the page"
      (let [ten-minutes-ago (-> -10 c/minutes c/from-now)
            ten-minutes-ago-str (f/unparse (f/formatters :date-time) ten-minutes-ago)
            page (i/index {:context {:activities
                                     [{:activity-src :an-activity-src
                                       "@context" "http://www.w3.org/ns/activitystreams"
                                       "@type" "Create"
                                       "published" ten-minutes-ago-str
                                       "actor"  {"@type" "Person"
                                                 "displayName" "JDog"}
                                       "object"  {"@type" "Objective"
                                                  "displayName" "OBJECTIVE 7 TITLE"
                                                  "content" "We want to establish Activity Types for Objective8"
                                                  "url" "http://objective8.dcentproject.eu/objectives/7"}}
                                      {:activity-src :another-activity-src
                                       "@context" "http://www.w3.org/ns/activitystreams"
                                       "@type" "Create"
                                       "published" "2015-08-04T14:49:38.407Z"
                                       "actor"  {"@type" "Person"
                                                 "displayName" "Lala"}
                                       "object"  {"@type" "Objective"
                                                  "displayName" "OBJECTIVE 6 TITLE"
                                                  "description" "Yes."
                                                  "url" "http://objective8.dcentproject.eu/objectives/6"}}]}})
            first-activity-item (first (html/select page [:.clj--activity-item]))
            second-activity-item (second (html/select page [:.clj--activity-item]))]

        (count (html/select page [:.clj--activity-item])) => 2

        first-activity-item => (th/has-attr? [:.clj--activity-item__link] :href "http://objective8.dcentproject.eu/objectives/7")
        first-activity-item => (th/has-attr? [:.clj--activity-item__time] :datetime ten-minutes-ago-str)
        first-activity-item => (th/text-is? [:.clj--activity-item__time] "10 minutes ago")
        first-activity-item => (th/text-is? [:.clj--activity-item__action] "JDog - Objective - Create")
        first-activity-item => (th/text-is? [:.clj--activity-item__title]  "OBJECTIVE 7 TITLE")

        second-activity-item => (th/has-attr? [:.clj--activity-item__link] :href "http://objective8.dcentproject.eu/objectives/6")
        second-activity-item => (th/has-attr? [:.clj--activity-item__time] :datetime "2015-08-04T14:49:38.407Z")
        second-activity-item => (th/text-is? [:.clj--activity-item__action]  "Lala - Objective - Create")
        second-activity-item => (th/text-is? [:.clj--activity-item__title]  "OBJECTIVE 6 TITLE")))

(fact "activity item avatars are given the initial of the actor (the name of the person)"
      (let [page (i/index {:context {:activities
                                     [{:activity-src :an-activity-src
                                       "actor"  {"@type" "Person"
                                                 "displayName" "abby"}}
                                      {:activity-src :an-activity-src
                                       "actor"  {"@type" "Person"
                                                 "displayName" "Bobby"}}
                                      {:activity-src :an-activity-src
                                       "actor"  {"@type" "Person"
                                                 "displayName" "2k12carlos"}}]}})
            initials-elements (-> (html/select page [:.clj--avatar__initials]))]
        (html/text (first initials-elements)) => "A"
        (html/text (second initials-elements)) => "B"
        (html/text (nth initials-elements 2 nil)) => "2"))

(fact  "activity item avatars are assigned the correct classes so they can be colour-coded by activity source"
      (let [page (i/index {:context {:activities
                                     [{:activity-src :an-activity-src}
                                      {:activity-src :another-activity-src}
                                      {:activity-src :an-activity-src}]}})
            first-activity-item-class (-> (html/select page [:.clj--activity-item])
                                          first :attrs :class)
            second-activity-item-class (-> (html/select page [:.clj--activity-item])
                                           second :attrs :class)
            third-activity-item-class (-> (html/select page [:.clj--activity-item])
                                          (nth 2 nil) :attrs :class)]
        first-activity-item-class =>      (contains "activity-src-0")
        first-activity-item-class =not=>  (contains "activity-src-1")
        (count (re-seq #"activity-src-" first-activity-item-class))  => 1
        second-activity-item-class =>     (contains "activity-src-1")
        second-activity-item-class =not=> (contains "activity-src-0")
        (count (re-seq #"activity-src-" second-activity-item-class))  => 1
        third-activity-item-class =>      (contains "activity-src-0")
        third-activity-item-class =not=>  (contains "activity-src-1")
        (count (re-seq #"activity-src-" third-activity-item-class))  => 1))

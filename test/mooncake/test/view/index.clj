(ns mooncake.test.view.index
  (:require [midje.sweet :refer :all]
            [net.cgrand.enlive-html :as html]
            [clojure.contrib.humanize :as h]
            [clj-time.core :as c]
            [clj-time.format :as f]
            [mooncake.view.index :as i]))

(fact "index page should return index template"
      (let [page (i/index :request)]
        (-> (html/select page [:body]) first :attrs :class) => "func--index-page"))

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

        (-> (html/select first-activity-item [:.clj--activity-item__link])
            first :attrs :href) => "http://objective8.dcentproject.eu/objectives/7"
        (-> (html/select first-activity-item [:.clj--activity-item__time])
            first :attrs :datetime) => ten-minutes-ago-str
        (-> (html/select first-activity-item [:.clj--activity-item__time])
            first html/text) => "10 minutes ago"
        (-> (html/select first-activity-item [:.clj--activity-item__action])
            first html/text) => "JDog - Objective - Create"
        (-> (html/select first-activity-item [:.clj--activity-item__title])
            first html/text) => "OBJECTIVE 7 TITLE"

        (-> (html/select second-activity-item [:.clj--activity-item__link])
            first :attrs :href) => "http://objective8.dcentproject.eu/objectives/6"
        (-> (html/select second-activity-item [:.clj--activity-item__time])
            first :attrs :datetime) => "2015-08-04T14:49:38.407Z"
        (-> (html/select second-activity-item [:.clj--activity-item__action])
            first html/text) => "Lala - Objective - Create"
        (-> (html/select second-activity-item [:.clj--activity-item__title])
            first html/text) => "OBJECTIVE 6 TITLE"))

(fact "activity item avatars are assigned the correct classes so they can be colour-coded by activity source"
      (let [page (i/index {:context {:activities
                                     [{:activity-src :an-activity-src}
                                      {:activity-src :another-activity-src}
                                      {:activity-src :an-activity-src}]}})
            first-activity-item-class (-> (html/select page [:.clj--activity-item])
                                          first :attrs :class)
            second-activity-item-class (-> (html/select page [:.clj--activity-item])
                                           second :attrs :class)
            third-activity-item-class (-> (html/select page [:.clj--activity-item])
                                          (nth 2) :attrs :class)]
        first-activity-item-class =>      (contains "activity-src-0")
        first-activity-item-class =not=>  (contains "activity-src-1")
        second-activity-item-class =>     (contains "activity-src-1")
        second-activity-item-class =not=> (contains "activity-src-0")
        third-activity-item-class =>      (contains "activity-src-0")
        third-activity-item-class =not=>  (contains "activity-src-1")))

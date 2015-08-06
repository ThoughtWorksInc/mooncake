(ns mooncake.test.view.index
  (:require [midje.sweet :refer :all]
            [net.cgrand.enlive-html :as html]
            [mooncake.view.index :as i]))

(fact "index page should return index template"
      (let [page (i/index :request)]
        (-> (html/select page [:body]) first :attrs :class) => "func--index-page"))

(fact "activities are rendered on the page"
      (let [page (i/index {:context {:activities
                                     [{"@context" "http://www.w3.org/ns/activitystreams"
                                       "@type" "Create"
                                       "published" "2015-08-05T11:17:51.181Z"
                                       "actor"  {"@type" "Person"
                                                 "displayName" "JDog"}
                                       "object"  {"@type" "Objective"
                                                  "displayName" "OBJECTIVE 7 TITLE"
                                                  "content" "We want to establish Activity Types for Objective8"
                                                  "url" "http://objective8.dcentproject.eu/objectives/7"}}
                                      {"@context" "http://www.w3.org/ns/activitystreams"
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
            first :attrs :datetime) => "2015-08-05T11:17:51.181Z"
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

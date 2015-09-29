(ns mooncake.test.controller.feed
  (:require [midje.sweet :refer :all]
            [mooncake.controller.feed :as fc]
            [mooncake.test.test-helpers.enlive :as eh]
            [mooncake.test.test-helpers.db :as dbh]
            [mooncake.db.user :as user]
            [mooncake.db.mongo :as mongo]
            [mooncake.db.activity :as a]))

(def ten-oclock "2015-01-01T10:00:00.000Z")
(def eleven-oclock "2015-01-01T11:00:00.000Z")
(def twelve-oclock "2015-01-01T12:00:00.000Z")

(fact "feed handler displays activities retrieved from activity sources"
      (let [database (dbh/create-in-memory-db)]
        (mongo/store! database a/activity-collection {"actor"       {"@type"       "Person"
                                                                     "displayName" "JDog"}
                                                      "published"   ten-oclock
                                                      "activity-src" "OpenAhjo"
                                                      "@type"       "Activity"})
        (mongo/store! database a/activity-collection {"actor"       {"@type"       "Person"
                                                                     "displayName" "KCat"}
                                                      "published"   twelve-oclock
                                                      "activity-src" "objective8"
                                                      "@type"       "Activity"})
        (fc/feed database {:context
                           {:activity-sources {:OpenAhjo   {:activity-types ["Activity"]}
                                               :objective8 {:activity-types ["Activity"]}}
                            :translator       (constantly "")}}) => (every-checker
                                                                      (eh/check-renders-page :.func--feed-page)
                                                                      (contains {:body (contains "JDog")})
                                                                      (contains {:body (contains "KCat")}))))

(fact "feed handler displays username of logged-in user"
      (fc/feed (dbh/create-in-memory-db) {:context {:translator (constantly "")}
                                          :session {:username "Barry"}}) => (contains {:body (contains "Barry")}))

(def activity-src-1--enabled-type {"actor"        {"@type"       "Person"
                                                   "displayName" "Activity source 1: enabled type"}
                                   "published"    ten-oclock
                                   "activity-src" "activity-src-1"
                                   "@type"        "Enabled"})
(def activity-src-1--disabled-type {"actor"        {"@type"       "Person"
                                                    "displayName" "Activity source 1: disabled type"}
                                    "published"    eleven-oclock
                                    "activity-src" "activity-src-1"
                                    "@type"        "Disabled"})
(def activity-src-2--no-preference-type {"actor"        {"@type"       "Person"
                                                         "displayName" "Activity source 2: no preference expressed"}
                                         "published"    twelve-oclock
                                         "activity-src" "activity-src-2"
                                         "@type"        "No-preference"})

(facts "about which activities feed handler displays"
       (let [database (dbh/create-in-memory-db)
             _ (mongo/store! database a/activity-collection activity-src-1--enabled-type)
             _ (mongo/store! database a/activity-collection activity-src-1--disabled-type)
             _ (mongo/store! database a/activity-collection activity-src-2--no-preference-type)
             _ (user/create-user! database ...user-id... ...username...)
             _ (user/update-feed-settings! database ...username... {:activity-src-1 {:types [{:id "Enabled" :selected true}
                                                                                             {:id "Disabled" :selected false}]}})
             request {:context {:activity-sources {:activity-src-1 {:activity-types ["Enabled" "Disabled"]}
                                                   :activity-src-2 {:activity-types ["No-preference"]}}
                                :translator       (constantly "")}
                      :session {:username ...username...}}
             response (fc/feed database request)]


         (fact "enabled activity types are shown"
               (:body response) => (contains "Activity source 1: enabled type"))

         (fact "disabled activity types are not shown"
               (:body response) =not=> (contains "Activity source 1: disabled type"))

         (fact "no-preference activity types are shown"
               (:body response) => (contains "Activity source 2: no preference expressed"))

         (fact "custom message is not shown when there are activities on the page"
               (:body response) =not=> (contains "clj--empty-activity-item"))

         (fact "custom message is shown if all activity types are disabled"
               (user/update-feed-settings! database ...username... {:activity-src-1 {:types [{:id "Disabled" :selected false}]}})
               (let [no-activities-request {:context {:activity-sources {:activity-src-1 {:activity-types ["Disabled"]}}
                                                      :translator       (constantly "")}
                                            :session {:username ...username...}}
                     no-activities-response (fc/feed database no-activities-request)]
                 (:body no-activities-response) => (contains "clj--empty-activity-item")))))

(facts "about generating the activity query map"
       (let [feed-settings {:activity-src-1  {:types    [{:id "Enabled" :selected true}
                                                         {:id "Disabled" :selected false}]}
                            :activity-src-2 {:types    [{:id "Disabled" :selected false}]}}

             activity-sources {:activity-src-1 {:activity-types ["Enabled" "Disabled" "No-preference"]}
                               :activity-src-2 {:activity-types ["Disabled"]}
                               :activity-src-3 {:activity-types ["No-preference"]}}]

         (fc/generate-feed-query feed-settings activity-sources) => (just [{"activity-src" "activity-src-1"
                                                                            "@type" ["Enabled" "No-preference"]}
                                                                           {"activity-src" "activity-src-3"
                                                                            "@type" ["No-preference"]}] :in-any-order)))
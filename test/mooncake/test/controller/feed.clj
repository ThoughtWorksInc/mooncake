(ns mooncake.test.controller.feed
  (:require [midje.sweet :refer :all]
            [clojure.walk :as w]
            [mooncake.controller.feed :as fc]
            [mooncake.test.test-helpers.enlive :as eh]
            [mooncake.test.test-helpers.db :as dbh]
            [mooncake.db.user :as user]
            [mooncake.db.mongo :as mongo]
            [mooncake.db.activity :as a]))

(def ten-oclock "2015-01-01T10:00:00.000Z")
(def eleven-oclock "2015-01-01T11:00:00.000Z")
(def twelve-oclock "2015-01-01T12:00:00.000Z")



;(def str-source-a-activity (w/stringify-keys source-a-type-1-activity))
;(def str-source-b-activity (w/stringify-keys source-b-activity))
;(def str-source-c-activity (w/stringify-keys source-c-activity))

; retrieve all activities of the type that user has expl requested
; don't retrieve activities of the type tha user has expl hidden
; retrieve all activities of type that user has not expressed preferences about for sources the user has requested

; don't retrieve any activities for a source that user has explicitly hidden
; retrieve all activities for a source that user has not expressed the pref about

(fact "feed handler displays activities retrieved from activity sources"
      (let [database (dbh/create-in-memory-db)]
        (mongo/store! database a/activity-collection {:actor        {"@type"       "Person"
                                                                     "displayName" "JDog"}
                                                      :published    ten-oclock
                                                      :activity-src "OpenAhjo"
                                                      "@type"       "Activity"})
        (mongo/store! database a/activity-collection {:actor            {"@type"       "Person"
                                                                         "displayName" "KCat"}
                                                      :published        twelve-oclock
                                                      :activity-src     "objective8"
                                                      (keyword "@type") "Activity"})
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

(def enabled-source--enabled-type {:actor        {"@type"       "Person"
                                                  "displayName" "Enabled source: enabled type"}
                                   :published    ten-oclock
                                   :activity-src "enabled"
                                   "@type"       "Enabled"})
(def enabled-source--disabled-type {:actor        {"@type"       "Person"
                                                   "displayName" "Enabled source: disabled type"}
                                    :published    eleven-oclock
                                    :activity-src "enabled"
                                    "@type"       "Disabled"})
(def enabled-source--no-preference-type {:actor        {"@type"       "Person"
                                                        "displayName" "Enabled source: no preference expressed"}
                                         :published    twelve-oclock
                                         :activity-src "enabled"
                                         "@type"       "No-preference"})

(def disabled-source--disabled-type {:actor        {"@type"       "Person"
                                                    "displayName" "Disabled source"}
                                     :published    ten-oclock
                                     :activity-src "disabled"
                                     "@type"       "Disabled"})

(def disabled-source--no-preference-type {:actor        {"@type"       "Person"
                                                         "displayName" "Disabled source"}
                                          :published    eleven-oclock
                                          :activity-src "disabled"
                                          "@type"       "No-preference"})

(def no-preference-source--some-type {:actor        {"@type"       "Person"
                                                     "displayName" "No preference expressed: any type"}
                                      :published    ten-oclock
                                      :activity-src "no-preference"
                                      "@type"       "Some-type"})

(facts "about which activities feed handler displays"
       (let [database (dbh/create-in-memory-db)
             _ (mongo/store! database a/activity-collection enabled-source--enabled-type)
             _ (mongo/store! database a/activity-collection enabled-source--disabled-type)
             _ (mongo/store! database a/activity-collection enabled-source--no-preference-type)
             _ (mongo/store! database a/activity-collection disabled-source--disabled-type)
             _ (mongo/store! database a/activity-collection disabled-source--no-preference-type)
             _ (mongo/store! database a/activity-collection no-preference-source--some-type)
             _ (user/create-user! database ...user-id... ...username...)
             _ (user/update-feed-settings! database ...username... {:enabled  {:selected true
                                                                               :types    [{:id "Enabled" :selected true}
                                                                                          {:id "Disabled" :selected false}]}
                                                                    :disabled {:selected false
                                                                               :types    [{:id "Type-1" :selected false}]}})
             request {:context {:activity-sources {:enabled       {:activity-types ["Enabled" "Disabled" "No-preference"]}
                                                   :disabled      {:activity-types ["Disabled" "No-preference"]}
                                                   :no-preference {:activity-types ["Some-type"]}}
                                :translator       (constantly "")}
                      :session {:username ...username...}}
             response (fc/feed database request)]


         (facts "activities from explicitly enabled sources"
                (fact "with explicitly enabled type are displayed"
                      (:body response) => (contains "Enabled source: enabled type"))
                (fact "with explicitly hidden type are not displayed"
                      (:body response) =not=> (contains "Enabled source: disabled type"))
                (fact "with types for which the user has not expressed a preference are displayed"
                      (:body response) => (contains "Enabled source: no preference expressed")))

         (fact "activities from explicitly disabled activity sources are not shown"
               (:body response) =not=> (contains "Disabled source"))

         (fact "all activities from sources user has not expressed preferences about are displayed"
               (:body response) => (contains "No preference expressed: any type"))

         (fact "custom message is not shown if any activity sources are enabled"
               (let [response (fc/feed database request)]
                 (:body response) =not=> (contains "clj--empty-activity-item")))

         (fact "custom message is shown if all activity sources are disabled"
               (user/update-feed-settings! database ...username... {:enabled       {:selected false}
                                                                    :disabled      {:selected false}
                                                                    :no-preference {:selected false}})
               (let [response (fc/feed database request)]
                 (:body response) => (contains "clj--empty-activity-item")))))

(tabular
    (fact "about retrieve-activities-from-user-sources"
          (let [activity-sources {:source-a {} :source-b {} :source-c {}}]
            (fc/get-active-activity-source-keys ?user-feed-settings activity-sources) => ?active-activity-source-keys))
    ?user-feed-settings ?active-activity-source-keys
    nil ["source-a" "source-b" "source-c"]
    {} ["source-a" "source-b" "source-c"]
    {:source-c {:selected true}} ["source-a" "source-b" "source-c"]
    {:source-a {:selected true} :source-b {:selected true}} ["source-a" "source-b" "source-c"]
    {:source-a {:selected true} :source-b {:selected true} :source-c {:selected true}} ["source-a" "source-b" "source-c"]
    {:source-a {:selected true} :source-b {:selected false} :source-c {:selected true}} ["source-a" "source-c"]
    {:source-a {:selected false} :source-b {:selected false} :source-c {:selected false}} [])

(facts "about generating the activity query map"
       (let [feed-settings {:enabled  {:selected true
                                       :types    [{:id "Enabled" :selected true}
                                                  {:id "Disabled" :selected false}]}
                            :disabled {:selected false
                                       :types    [{:id "Type-1" :selected false}]}}

             activity-sources {:enabled       {:activity-types ["Enabled" "Disabled" "No-preference"]}
                               :disabled      {:activity-types ["Disabled" "No-preference"]}
                               :no-preference {:activity-types ["Some-type"]}}]

         (fc/generate-feed-query feed-settings activity-sources) => (just [{"activity-src" "enabled"
                                                                            "@type" ["Enabled" "No-preference"]}
                                                                           {"activity-src" "no-preference"
                                                                            "@type" ["Some-type"]}] :in-any-order)

         (facts "for enabled sources"
                (fact "enabled activity types are present"
                      (fc/generate-feed-query {:enabled {:selected true :types [{:id "Enabled" :selected true}]}}
                                              {:enabled {:activity-types ["Enabled"]}})
                      => (just [{"activity-src" "enabled"
                                 "@type"        ["Enabled"]}]))

                (fact "no-preference activity types are present"
                      (fc/generate-feed-query {:enabled {:selected true :types []}}
                                              {:enabled {:activity-types ["No-preference"]}})
                      => (just [{"activity-src" "enabled"
                                 "@type"        ["No-preference"]}]))

                (fact "disabled activity types are not present"
                      (fc/generate-feed-query {:enabled {:selected true :types [{:id "Disabled" :selected false}]}}
                                              {:enabled {:activity-types ["Disabled"]}})
                      => (just [{"activity-src" "enabled"
                                 "@type"       []}])))

         (fact "for disabled sources, activity types are not present"
               (fc/generate-feed-query {:disabled {:selected false :types [{:id "A-type" :selected false}]}}
                                       {:disabled {:activity-types ["A-type"]}})
               => empty?)


         (fact "for no-preference sources, activity types are present"
               (fc/generate-feed-query {}
                                       {:no-preference {:activity-types ["A-type"]}})
               => (just [{"activity-src" "no-preference"
                          "@type"        ["A-type"]}]))))
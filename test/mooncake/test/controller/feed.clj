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

(def source-a-activity {:actor        {"@type"       "Person"
                                       "displayName" "Author A"}
                        :published    ten-oclock
                        :activity-src "source-a"})
(def source-b-activity {:actor        {"@type"       "Person"
                                       "displayName" "Author B"}
                        :published    eleven-oclock
                        :activity-src "source-b"})
(def source-c-activity {:actor        {"@type"       "Person"
                                       "displayName" "Author C"}
                        :published    twelve-oclock
                        :activity-src "source-c"})

(def str-source-a-activity (w/stringify-keys source-a-activity))
(def str-source-b-activity (w/stringify-keys source-b-activity))
(def str-source-c-activity (w/stringify-keys source-c-activity))


(fact "feed handler displays activities retrieved from activity sources"
      (let [database (dbh/create-in-memory-db)]
        (mongo/store! database a/activity-collection {:actor     {"@type"       "Person"
                                                                   "displayName" "JDog"}
                                                      :published ten-oclock
                                                      :activity-src  "OpenAhjo"})
        (mongo/store! database a/activity-collection {:actor     {"@type"       "Person"
                                                                   "displayName" "KCat"}
                                                      :published twelve-oclock
                                                      :activity-src  "objective8"})
        (fc/feed database {:context
                           {:activity-sources {:OpenAhjo {} :objective8 {}}
                            :translator (constantly "")}}) => (every-checker
                                                                (eh/check-renders-page :.func--feed-page)
                                                                (contains {:body (contains "JDog")})
                                                                (contains {:body (contains "KCat")}))))

(fact "feed handler displays username of logged-in user"
      (fc/feed (dbh/create-in-memory-db) {:context {:translator (constantly "")}
                                          :session {:username "Barry"}}) => (contains {:body (contains "Barry")}))

(facts "about which activities feed handler displays"
       (let [database (dbh/create-in-memory-db)
             _ (mongo/store! database a/activity-collection source-a-activity)
             _ (mongo/store! database a/activity-collection source-b-activity)
             _ (mongo/store! database a/activity-collection source-c-activity)
             _ (user/create-user! database ...user-id... ...username...)
             _ (user/update-feed-settings! database ...username... {:source-a true :source-b false})
             request {:context {:activity-sources {:source-a {} :source-b {} :source-c {}}
                                :translator       (constantly "")}
                      :session {:username ...username...}}
             response (fc/feed database request)]
         (fact "activities from enabled activity sources are shown"
               (:body response) => (contains "Author A"))
         (fact "activities from disabled activity sources are not shown"
               (:body response) =not=> (contains "Author B"))
         (fact "custom message is shown if all activity sources are disabled"
               (user/update-feed-settings! database ...username... {:source-a false :source-b false :source-c false})
               (let [response (fc/feed database request)]
                 (:body response) => (contains "clj--empty-activity-item")))
         (fact "custom message is not shown if any activity sources are enabled"
               (user/update-feed-settings! database ...username... {:source-a false :source-b false :source-c true})
               (let [response (fc/feed database request)]
                 (:body response) =not=> (contains "clj--empty-activity-item")))))

(tabular
  (fact "about retrieve-activities-from-user-sources"
        (let [activity-sources {:source-a {} :source-b {} :source-c {}}]
          (fc/get-active-activity-source-keys ?user-feed-settings activity-sources) => ?active-activity-source-keys))
  ?user-feed-settings                                  ?active-activity-source-keys
  nil                                                  ["source-a" "source-b" "source-c"]
  {}                                                   ["source-a" "source-b" "source-c"]
  {:source-c true}                                     ["source-a" "source-b" "source-c"]
  {:source-a true :source-b true}                      ["source-a" "source-b" "source-c"]
  {:source-a true :source-b true  :source-c true}      ["source-a" "source-b" "source-c"]
  {:source-a true :source-b false :source-c true}      ["source-a"  "source-c"]
  {:source-a false :source-b false :source-c false}    [])

(tabular
  (fact "about retrieve-activities-from-user-sources"
        (let [database (dbh/create-in-memory-db)
              _ (mongo/store! database a/activity-collection source-a-activity)
              _ (mongo/store! database a/activity-collection source-b-activity)
              _ (mongo/store! database a/activity-collection source-c-activity)]
          (fc/retrieve-activities-from-user-sources database ?active-activity-source-keys) => ?activities))
  ?active-activity-source-keys             ?activities
  nil                                      []
  []                                       []
  ["source-a"]                             [str-source-a-activity]
  ["source-a" "source-c"]                  [str-source-c-activity str-source-a-activity]
  ["source-a" "source-b" "source-c"]       [str-source-c-activity str-source-b-activity str-source-a-activity])
(ns mooncake.test.controller.feed
  (:require [midje.sweet :refer :all]
            [mooncake.controller.feed :as fc]
            [mooncake.test.test-helpers.enlive :as eh]
            [mooncake.test.test-helpers.db :as dbh]
            [mooncake.db.mongo :as mongo]
            [mooncake.db.activity :as a]))

(def ten-oclock "2015-01-01T10:00:00.000Z")
(def twelve-oclock "2015-01-01T12:00:00.000Z")

(fact "feed handler displays activities retrieved from activity sources"
      (let [database (dbh/create-in-memory-db)]
        (mongo/store! database a/activity-collection {"actor"     {"@type"       "Person"
                                                                   "displayName" "JDog"}
                                                      "published" ten-oclock
                                                      "activity-src" "OpenAhjo"})
        (mongo/store! database a/activity-collection {"actor"     {"@type"       "Person"
                                                                   "displayName" "KCat"}
                                                      "published" twelve-oclock
                                                      "activity-src" "objective8"})
        (fc/feed database {:context
                           {:translator (constantly "")}}) => (every-checker
                                                                (eh/check-renders-page :.func--feed-page)
                                                                (contains {:body (contains "JDog")})
                                                                (contains {:body (contains "KCat")}))))

(fact "feed handler displays username of logged-in user"
      (fc/feed (dbh/create-in-memory-db) {:context {:translator (constantly "")}
                                          :session {:username "Barry"}}) => (contains {:body (contains "Barry")}))
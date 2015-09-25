(ns mooncake.integration.db.user
  (:require [midje.sweet :refer :all]

            [mooncake.test.test-helpers.db :as dbh]
            [mooncake.db.mongo :as mongo]
            [mooncake.db.user :as user]))


(fact "can store user feed settings"
      (dbh/with-mongo-do
        (fn [db]
          (let [database (mongo/create-database db)
                stored-user (user/create-user! database "userid" "username")
                feed-settings {:activity-src true
                               :another-activity-src false}
                expected-user (assoc stored-user :feed-settings feed-settings)]
            (user/update-feed-settings! database "username" feed-settings)
            (mongo/find-item database user/collection {:username "username"}) => expected-user))))
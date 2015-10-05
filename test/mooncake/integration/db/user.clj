(ns mooncake.integration.db.user
  (:require [midje.sweet :refer :all]
            [mooncake.test.test-helpers.db :as dbh]
            [mooncake.db.mongo :as mongo]
            [mooncake.db.user :as user]))

(fact "can store user feed settings"
      (dbh/with-mongo-do
        (fn [db]
          (let [store (mongo/create-mongo-store db)
                stored-user (user/create-user! store "userid" "username")
                feed-settings {:activity-src true
                               :another-activity-src false}
                expected-user (assoc stored-user :feed-settings feed-settings)]
            (user/update-feed-settings! store "username" feed-settings)
            (mongo/find-item store user/collection {:username "username"} {:stringify? false}) => expected-user))))
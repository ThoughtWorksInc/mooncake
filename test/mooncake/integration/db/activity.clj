(ns mooncake.integration.db.activity
  (:require [midje.sweet :refer :all]
            [mooncake.db.activity :as activity]
            [mooncake.test.test-helpers.db :as dbh]
            [mooncake.db.mongo :as mongo]))

(background (before :facts (dbh/drop-db!)))

(fact "can store an activity"
      (dbh/with-mongo-do
        (fn [db]
          (let [database (mongo/create-database db)
                activity {"displayName" "KCat"}]
            (activity/store-activity! database activity)
            (mongo/find-item database activity/collection {"displayName" "KCat"} true) => {:displayName "KCat"}))))



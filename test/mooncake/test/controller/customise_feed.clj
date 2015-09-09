(ns mooncake.test.controller.customise-feed
  (:require [midje.sweet :refer :all]
            [ring.mock.request :as mock]
            [mooncake.controller.customise-feed :as cf]
            [mooncake.db.user :as user]
            [mooncake.routes :as routes]
            [mooncake.test.test-helpers.enlive :as eh]
            [mooncake.test.test-helpers.db :as dbh]))

(facts "about customise-feed"
       (facts "when successful"
              (let [customise-feed-request {:context {:activity-sources {:activity-src "a url"
                                                                         :another-activity-src "another url"}}
                                            :params  {:activity-src "true"}
                                            :session {:username ...username...}}
                    db (dbh/create-in-memory-db)
                    stored-user (user/create-user! db ...user-id... ...username...)
                    response (cf/customise-feed db customise-feed-request)]
                (fact "it should update the user's feed settings"
                      (user/find-user db ...username...) => {:auth-provider-user-id ...user-id...
                                                             :username              ...username...
                                                             :feed-settings {:activity-src true}})
                (future-fact "it should redirect to /"
                      response => (eh/check-redirects-to (routes/absolute-path {} :index))))))

(ns mooncake.test.handler
  (:require [midje.sweet :refer :all]
            [clj-http.client :as http]
            [stonecutter-oauth.client :as soc]
            [mooncake.db.mongo :as mongo]
            [mooncake.handler :as h]
            [mooncake.routes :as routes]
            [mooncake.test.test-helpers :as th]))


(def ten-oclock "2015-01-01T10:00:00.000Z")
(def eleven-oclock "2015-01-01T11:00:00.000Z")
(def twelve-oclock "2015-01-01T12:00:00.000Z")

(facts "about site-handlers"
       (fact "throws an exception when stonecutter oauth configuration is invalid"
             (h/site-handlers {} nil) => (throws anything)
             (provided
               (h/create-stonecutter-config anything) => :invalid-configuration)))

(fact "index handler displays activities retrieved from activity sources"
      (let [an-activity-src-url "https://an-activity.src"
            another-activity-src-url "https://another-activity.src"]
        (h/index {:context
                  {:translator (constantly "")
                   :activity-sources
                   {:an-activity-src an-activity-src-url
                    :another-activity-src another-activity-src-url}}}) => (every-checker
                                                                            (contains {:status 200})
                                                                            (contains {:body (contains "JDog")})
                                                                            (contains {:body (contains "KCat")}))
        (provided
          (http/get an-activity-src-url {:accept :json
                                         :as :json-string-keys})       => {:body [{"actor" {"@type" "Person"
                                                                                            "displayName" "JDog"}
                                                                                   "published" ten-oclock}]}
          (http/get another-activity-src-url {:accept :json
                                              :as :json-string-keys})  => {:body [{"actor" {"@type" "Person"
                                                                                            "displayName" "KCat"}
                                                                                   "published" twelve-oclock}]})))

(defrecord NoUserStoreTestHelper []
  mongo/database
  (fetch [this user-id]
    nil))

(defrecord UserStoreTestHelper [name]
  mongo/database
  (fetch [this user-id]
    {:id user-id :name name}))

(fact "sign-in handler redirects to / when user is signed in and has a user name"
      (h/sign-in (UserStoreTestHelper. "Bob") {:session {:user-id ...user-id...}}) => (th/check-redirects-to (routes/absolute-path {} :index)))

(fact "sign-in handler redirects to create-account when user is signed in but there is no document for the user in the db"
      (h/sign-in (NoUserStoreTestHelper.) {:session {:user-id ...user-id...}}) 
          => (th/check-redirects-to (routes/absolute-path {} :create-account)))

(fact "sign-in handler redirects to create-account when user is signed in but has no user name"
      (h/sign-in (UserStoreTestHelper. nil) {:session {:user-id ...user-id...}}) 
          => (th/check-redirects-to (routes/absolute-path {} :create-account)))

(fact "stonecutter-sign-in handler delegates to the stonecutter client library"
      (h/stonecutter-sign-in ...stonecutter-config... ...request...) => ...stonecutter-sign-in-redirect...
      (provided
        (soc/authorisation-redirect-response ...stonecutter-config...) => ...stonecutter-sign-in-redirect...))

(facts "about stonecutter-callback"
       (fact "redirects to / with the user-id set in the session"
             (h/stonecutter-callback ...stonecutter-config... {:params {:code ...auth-code...}})
             => (every-checker
                  (th/check-redirects-to (routes/absolute-path {} :index))
                  (contains {:session {:user-id ...stonecutter-user-id...}}))
             (provided
               (soc/request-access-token! ...stonecutter-config... ...auth-code...)
               => {:user-info {:sub ...stonecutter-user-id...}}))

       (fact "passes on stonecutter oauth client exception"
             (h/stonecutter-callback ...stonecutter-config... {:params {:code ...auth-code...}})
             => (throws Exception)
             (provided
               (soc/request-access-token! anything anything) =throws=> (ex-info "Invalid token response" {:token-response-keys []}))))

(fact "sign-out handler clears the session and redirects to /sign-in"
      (let [response (h/sign-out {:session {:user-id ...some-user-id...
                                            :some-other-key ...some-other-value...}})]
        (:session response) => {}
        response => (th/check-redirects-to (routes/absolute-path {} :sign-in))))

(future-facts "about create-account"
       (fact "when navigating to create-account should render the page"
             
             )
       )

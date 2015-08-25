(ns mooncake.test.handler
  (:require [midje.sweet :refer :all]
            [clj-http.client :as http]
            [stonecutter-oauth.client :as soc]
            [mooncake.db.user :as user]
            [mooncake.handler :as h]
            [mooncake.routes :as routes]
            [mooncake.test.test-helpers.enlive :as eh]))


(def ten-oclock "2015-01-01T10:00:00.000Z")
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
                                                                            (eh/check-renders-page :.func--index-page)
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
(fact "index handler displays username of logged-in user"
      (h/index {:context {:translator (constantly "")}
                :session {:username "Barry"}}) => (contains {:body (contains "Barry")}))

(fact "sign-in handler renders the sign-in view when the user is not signed in"
      (h/sign-in {:context {:translator {}}}) => (eh/check-renders-page :.func--sign-in-page))

(fact "sign-in handler redirects to / when user is signed in"
      (h/sign-in {:session {:username ...username...}}) => (eh/check-redirects-to (routes/absolute-path {} :index)))

(fact "stonecutter-sign-in handler delegates to the stonecutter client library"
      (h/stonecutter-sign-in ...stonecutter-config... ...request...) => ...stonecutter-sign-in-redirect...
      (provided
        (soc/authorisation-redirect-response ...stonecutter-config...) => ...stonecutter-sign-in-redirect...))

(facts "about stonecutter-callback"
       (facts "when successfully authenticated"
              (fact "when new user, redirects to /create-account with the auth-provider-user-id set in the session"
                    (h/stonecutter-callback ...stonecutter-config... ...db...
                                            {:params {:code ...auth-code...}})
                    => (every-checker
                         (eh/check-redirects-to (routes/absolute-path {} :show-create-account))
                         (contains {:session {:auth-provider-user-id ...stonecutter-user-id...}}))
                    (provided
                      (soc/request-access-token! ...stonecutter-config... ...auth-code...)
                      => {:user-info {:sub ...stonecutter-user-id...}}
                      (user/fetch-user ...db... ...stonecutter-user-id...) => nil))

              (fact "when existing user, redirects to / with the username set in the session and auth-provider-user-id removed"
                    (h/stonecutter-callback ...stonecutter-config...  ...db...
                                            {:params {:code ...auth-code...}})
                    => (every-checker
                         (eh/check-redirects-to (routes/absolute-path {} :index))
                         (contains {:session {:username ...username...}}))
                    (provided
                      (soc/request-access-token! ...stonecutter-config... ...auth-code...)
                      => {:user-info {:sub ...stonecutter-user-id...}}
                      (user/fetch-user ...db... ...stonecutter-user-id...)
                      => {:username ...username... :auth-provider-user-id ...stonecutter-user-id...})))

       (fact "passes on stonecutter oauth client exception"
             (h/stonecutter-callback ...stonecutter-config... ...user-store... {:params {:code ...auth-code...}})
             => (throws Exception)
             (provided
               (soc/request-access-token! anything anything) =throws=> (ex-info "Invalid token response" {:token-response-keys []}))))

(fact "sign-out handler clears the session and redirects to /sign-in"
      (let [response (h/sign-out {:session {:user-id ...some-user-id...
                                            :some-other-key ...some-other-value...}})]
        (:session response) => {}
        response => (eh/check-redirects-to (routes/absolute-path {} :sign-in))))


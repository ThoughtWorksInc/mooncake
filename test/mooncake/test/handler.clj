(ns mooncake.test.handler
  (:require [midje.sweet :refer :all]
            [clj-http.client :as http]
            [cheshire.core :as json]
            [stonecutter-oauth.client :as soc]
            [stonecutter-oauth.jwt :as so-jwt]
            [mooncake.db.user :as user]
            [mooncake.handler :as h]
            [mooncake.routes :as routes]
            [mooncake.test.test-helpers.enlive :as eh]
            [mooncake.test.test-helpers.db :as dbh]
            [mooncake.db.mongo :as mongo]
            [mooncake.db.activity :as a]))


(def ten-oclock "2015-01-01T10:00:00.000Z")
(def twelve-oclock "2015-01-01T12:00:00.000Z")

(facts "about site-handlers"
       (fact "throws an exception when stonecutter oauth configuration is invalid"
             (h/site-handlers {} nil) => (throws anything)
             (provided
               (h/create-stonecutter-config anything) => :invalid-configuration)))

(fact "create-stonecutter-config configures stonecutter-oauth client to use openid"
      (h/create-stonecutter-config {:auth-url ...auth-url...
                                    :client-id ...client-id...
                                    :client-secret ...client-secret...}) => ...stonecutter-config-m...
      (provided
        (soc/configure ...auth-url... ...client-id... ...client-secret... anything :protocol :openid)
        => ...stonecutter-config-m...))

(fact "index handler displays activities retrieved from activity sources"
      (let [an-activity-src-url "https://an-activity.src"
            another-activity-src-url "https://another-activity.src"
            database (dbh/create-in-memory-db)]
        (mongo/store! database a/activity-collection {"actor"     {"@type"       "Person"
                                                                   "displayName" "JDog"}
                                                      "published" ten-oclock})
        (mongo/store! database a/activity-collection {"actor"     {"@type"       "Person"
                                                                   "displayName" "KCat"}
                                                      "published" twelve-oclock})
        (h/index database {:context
                           {:translator (constantly "")
                            :activity-sources
                                        {:an-activity-src      an-activity-src-url
                                         :another-activity-src another-activity-src-url}}}) => (every-checker
                                                                                                 (eh/check-renders-page :.func--index-page)
                                                                                                 (contains {:body (contains "JDog")})
                                                                                                 (contains {:body (contains "KCat")}))))
(fact "index handler displays username of logged-in user"
      (h/index (dbh/create-in-memory-db) {:context {:translator (constantly "")}
                                          :session {:username "Barry"}}) => (contains {:body (contains "Barry")}))

(fact "sign-in handler renders the sign-in view when the user is not signed in"
      (h/sign-in {:context {:translator {}}}) => (eh/check-renders-page :.func--sign-in-page))

(fact "sign-in handler redirects to / when user is signed in"
      (h/sign-in {:session {:username ...username...}}) => (eh/check-redirects-to (routes/absolute-path {} :index)))

(fact "stonecutter-sign-in handler delegates to the stonecutter client library"
      (h/stonecutter-sign-in ...stonecutter-config... ...request...) => ...stonecutter-sign-in-redirect...
      (provided
        (soc/authorisation-redirect-response ...stonecutter-config...) => ...stonecutter-sign-in-redirect...))

(def openid-test-config (soc/configure "ISSUER" "CLIENT_ID" "<client-secret>" "<callback-uri>"
                                       :protocol :openid))

(def test-auth-provider-public-key (slurp "./test-resources/test-key.json"))
(def test-auth-provider-public-key-as-clj-map (json/parse-string test-auth-provider-public-key))
(def token-expiring-in-year-2515 "eyJraWQiOiJ0ZXN0LWtleSIsImFsZyI6IlJTMjU2In0.eyJpc3MiOiJJU1NVRVIiLCJhdWQiOiJDTElFTlRfSUQiLCJleHAiOjE3MjA3OTkzMjUyLCJpYXQiOjE0Mzk5OTI3NDAsInN1YiI6IlNVQkpFQ1QiLCJyb2xlIjoiYWRtaW4iLCJlbWFpbF92ZXJpZmllZCI6dHJ1ZSwiZW1haWwiOiJlbWFpbEBhZGRyZXNzLmNvbSJ9.PQWWJQGECzC8EchkfwGjQBBUfhFGoLDOjZ1Ohl1t-eo8rXDO4FxONk3rYEY9v01fVg3pzQW8zLJYcZ73gyE2ju8feHhwS8wYwcsgKq6XC-Zr9LwRJIeFpZoVcgMpvW21UHX1bxAhHE7WM_UzSerKtGkIuK21XraGVTiIB-0o8eWOJX0Rud8FXC3Cr0LdZeqDytPZDwM1Pbcr0eFyfNq9ngi75BFNTGHCMLGshJGt1LvQhDtTWifXDlwW5uk-kuOVavnQGK_i7qvrcy8c7lFCCPqd5X3x6EZJyfk-BZGgDT1ySwdM2EjRAi1W1nPAmdWms9rts0rkbk_Q73gEkWQpOw")

;token-content is the decoded version of token-expiring-in-year-2515 signed with test-auth-provider-public-key
(def token-content {:aud "CLIENT_ID"
                    :email "email@address.com"
                    :email_verified true
                    :exp 17207993252
                    :iat 1439992740
                    :iss "ISSUER"
                    :role "admin"
                    :sub "SUBJECT"})

(facts "about stonecutter-callback"
       (facts "when successfully authenticated"
              (fact "when new user, redirects to /create-account with the auth-provider-user-id set in the session"
                    (h/stonecutter-callback openid-test-config ...db...
                                            {:params {:code ...auth-code...}})
                    => (every-checker
                         (eh/check-redirects-to (routes/absolute-path {} :show-create-account))
                         (contains {:session {:auth-provider-user-id "SUBJECT"}}))
                    (provided
                      (soc/request-access-token! openid-test-config ...auth-code...)
                      => {:id_token token-expiring-in-year-2515}
                      (http/get "ISSUER/api/jwk-set" {:accept :json :as :json})
                      => {:body {:keys [test-auth-provider-public-key-as-clj-map]}}
                      (user/fetch-user ...db... "SUBJECT") => nil))

              (fact "when existing user, redirects to / with the username set in the session and auth-provider-user-id removed"
                    (h/stonecutter-callback openid-test-config ...db...
                                            {:params {:code ...auth-code...}})
                    => (every-checker
                         (eh/check-redirects-to (routes/absolute-path {} :index))
                         (contains {:session {:username ...username...}}))
                    (provided
                      (soc/request-access-token! openid-test-config ...auth-code...)
                      => {:id_token token-expiring-in-year-2515}
                      (so-jwt/get-public-key-string-from-jwk-set-url "ISSUER/api/jwk-set")
                      => test-auth-provider-public-key
                      (user/fetch-user ...db... "SUBJECT")
                      => {:username ...username... :auth-provider-user-id "SUBJECT"})))

       (fact "passes on stonecutter oauth client exception"
             (h/stonecutter-callback ...stonecutter-config... ...user-store... {:params {:code ...auth-code...}})
             => (throws Exception)
             (provided
               (soc/request-access-token! anything anything) =throws=> (ex-info "Invalid token response" {:token-response-keys []}))))

(fact "sign-out handler clears the session and redirects to /sign-in"
      (let [response (h/sign-out {:session {:user-id        ...some-user-id...
                                            :some-other-key ...some-other-value...}})]
        (:session response) => {}
        response => (eh/check-redirects-to (routes/absolute-path {} :sign-in))))



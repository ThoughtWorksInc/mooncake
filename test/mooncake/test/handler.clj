(ns mooncake.test.handler
  (:require [midje.sweet :refer :all]
            [clj-http.client :as http]
            [stonecutter-oauth.client :as soc]
            [mooncake.routes :as routes]
            [mooncake.test.test-helpers :as th]
            [mooncake.handler :as h]))


(def ten-oclock "2015-01-01T10:00:00.000Z")
(def eleven-oclock "2015-01-01T11:00:00.000Z")
(def twelve-oclock "2015-01-01T12:00:00.000Z")

(facts "about site-handlers"
       (fact "throws an exception when stonecutter oauth configuration is invalid"
             (h/site-handlers {}) => (throws anything)
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
               => {:user-id ...stonecutter-user-id...})))

(fact "sign-out handler clears the session and redirects to /sign-in"
      (let [response (h/sign-out {:session {:user-id ...some-user-id...
                                            :some-other-key ...some-other-value...}})]
        (:session response) => {}
        response => (th/check-redirects-to (routes/absolute-path {} :sign-in))))

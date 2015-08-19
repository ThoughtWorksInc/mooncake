(ns mooncake.integration.kerodon
  (:require [midje.sweet :refer :all]
            [kerodon.core :as k]
            [ring.adapter.jetty :as ring-jetty]
            [stonecutter-oauth.client :as soc]
            [ring.util.response :as r]
            [mooncake.routes :as routes]
            [mooncake.handler :as h]
            [mooncake.config :as c]
            [mooncake.integration.kerodon-helpers :as kh]
            [mooncake.integration.kerodon-selectors :as ks]))

(defn print-enlive [state]
  (prn (-> state :enlive))
  state)

(defn print-request [state]
  (prn (-> state :request))
  state)

(defn print-state [state]
  (prn state)
  state)

(def server (atom nil))
(defn start-server [] (swap! server (fn [_] (ring-jetty/run-jetty
                                              (h/create-app (c/create-config) {})
                                              {:host "127.0.0.1" :port 3000 :join? false}))))
(defn stop-server [] (.stop @server))

(background
  (soc/request-access-token! anything anything) => {:user-info {:sub "test-stonecutter-user-uuid"}})

(def app (h/create-app (c/create-config) {}))

(defn sign-in-against-stub [state]
  (k/visit state "/d-cent-callback"))

(facts "The index page redirects to /sign-in when user is not signed in"
       (-> (k/session app)
           (k/visit "/")
           (kh/check-and-follow-redirect "sign-in")
           (kh/page-uri-is "/sign-in")
           (kh/response-status-is 200)))

(facts "A user can authenticate and create an account"
       (against-background
         (soc/authorisation-redirect-response anything) =>
         (r/redirect (routes/absolute-path (c/create-config) :stonecutter-callback)))
       (-> (k/session app)
           (k/visit "/sign-in")
           (kh/check-and-follow ks/sign-in-page-sign-in-with-d-cent-link)
           (kh/check-and-follow-redirect "to stonecutter")
           (kh/check-and-follow-redirect "to /create-account")
           (kh/page-uri-is "/create-account")
           (kh/response-status-is 200)))

(facts "A signed in user can sign out"
      (-> (k/session app)
          sign-in-against-stub
          (k/visit "/")
          (kh/check-and-follow ks/header-sign-out-link)
          (kh/check-and-follow-redirect "to sign-in page after signing out")
          (kh/page-uri-is "/sign-in")
          (kh/response-status-is 200)
          (kh/selector-not-present ks/header-sign-out-link)))

(against-background
  [(before :contents (start-server))
   (after :contents (stop-server))]
  (facts "Stub activities are rendered"
         (-> (k/session (h/create-app (c/create-config) {:stub-activity-source "http://127.0.0.1:3000/stub-activities"}))
             sign-in-against-stub
             (k/visit "/")
             (kh/page-uri-is "/")
             (kh/response-status-is 200)
             (kh/selector-exists ks/index-page-body)
             (kh/selector-includes-content ks/index-page-activity-item-title "Stub activity title")
             (kh/selector-includes-content ks/index-page-activity-item-action "Barry - STUB_ACTIVITY - Create")
             (kh/selector-has-attribute-with-content ks/index-page-activity-item-link :href "http://stub-activity.url"))))

(facts "Invalid activity source responses are handled gracefully"
       (-> (k/session (h/create-app (c/create-config) {:invalid-activity-src "http://localhost:6666/not-an-activity-source"}))
           sign-in-against-stub
           (k/visit "/")
           (kh/page-uri-is "/")
           (kh/response-status-is 200)))

(facts "Error page is shown if an exception is thrown"
       (against-background
         (h/sign-in anything) =throws=> (Exception.))
       (-> (k/session (h/create-app (c/create-config) {}))
           (k/visit "/sign-in")
           (kh/response-status-is 500)
           (kh/selector-exists ks/error-500-page-body)))

(facts "Going to an unknown uri renders the 404 page"
       (-> (k/session app)
           (k/visit "/not-a-valid-uri")
           (kh/page-uri-is "/not-a-valid-uri")
           (kh/response-status-is 404)
           (kh/selector-exists ks/error-404-page-body)))

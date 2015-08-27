(ns mooncake.integration.kerodon
  (:require [monger.core :as monger]
            [monger.db :as mdb]
            [midje.sweet :refer :all]
            [kerodon.core :as k]
            [ring.adapter.jetty :as ring-jetty]
            [stonecutter-oauth.client :as soc]
            [ring.util.response :as r]
            [mooncake.routes :as routes]
            [mooncake.handler :as h]
            [mooncake.config :as c]
            [mooncake.integration.kerodon-helpers :as kh]
            [mooncake.integration.kerodon-selectors :as ks]
            [mooncake.db.mongo :as mongo]))

(defn print-enlive [state]
  (clojure.pprint/pprint (-> state :enlive))
  state)

(defn print-request [state]
  (prn (-> state :request))
  state)

(defn print-state [state]
  (prn "=============== kerodon state ===============")
  (prn state)
  state)

(background
  (soc/request-access-token! anything anything) => {:user-info {:sub "test-stonecutter-user-uuid"}})

(def test-db-uri "mongodb://localhost:27017/mooncake")
(def database (mongo/create-database (mongo/get-mongo-db test-db-uri)))

(def app (h/create-app (c/create-config) database {}))

(defn drop-db! []
  (let [{:keys [conn db]} (monger/connect-via-uri test-db-uri)]
    (mdb/drop-db db)
    (monger/disconnect conn)))

(defn clean-app! []
  (drop-db!)
  app)

(defn authenticate-against-stub [state]
  (k/visit state (routes/absolute-path (c/create-config) :stonecutter-callback)))

(defn sign-in! [state]
  (-> state
      authenticate-against-stub
      (kh/check-and-follow-redirect "to /create-account")
      (kh/page-uri-is "/create-account")
      (kh/response-status-is 200)
      (kh/check-and-fill-in ks/create-account-page-username-input "Barry")
      (kh/check-and-press ks/create-account-page-submit-button)))

(facts "The index page redirects to /sign-in when user is not signed in"
       (-> (k/session app)
           (k/visit (routes/absolute-path (c/create-config) :index))
           (kh/check-and-follow-redirect "sign-in")
           (kh/page-uri-is "/sign-in")
           (kh/selector-exists ks/sign-in-page-body)
           (kh/response-status-is 200)))

(defn sign-out [state]
  (k/visit state (routes/absolute-path (c/create-config) :sign-out)))

(facts "A user can authenticate and create an account"
       (against-background
         (soc/authorisation-redirect-response anything) =>
         (r/redirect (routes/absolute-path (c/create-config) :stonecutter-callback)))
       (-> (k/session (clean-app!))
           (k/visit (routes/absolute-path (c/create-config) :sign-in))
           (kh/check-and-follow ks/sign-in-page-sign-in-with-d-cent-link)
           (kh/check-and-follow-redirect "to stonecutter")
           (kh/check-and-follow-redirect "to /create-account")
           (kh/page-uri-is "/create-account")
           (kh/response-status-is 200)
           (kh/check-and-fill-in ks/create-account-page-username-input "Barry")
           (kh/check-and-press ks/create-account-page-submit-button)
           (kh/check-and-follow-redirect "to /")
           (kh/page-uri-is "/")
           (kh/response-status-is 200)
           (kh/selector-exists ks/index-page-body)))

(facts "An existing user is redirected to / (rather than /create-account) after authenticating with stonecutter"
       (against-background
         (soc/authorisation-redirect-response anything) =>
         (r/redirect (routes/absolute-path (c/create-config) :stonecutter-callback)))
       (-> (k/session (clean-app!))
           sign-in!
           sign-out
           (k/visit (routes/absolute-path (c/create-config) :sign-in))
           (kh/check-and-follow ks/sign-in-page-sign-in-with-d-cent-link)
           (kh/check-and-follow-redirect "to stonecutter")
           (kh/check-and-follow-redirect "to /")
           (kh/page-uri-is "/")
           (kh/response-status-is 200)
           (kh/selector-exists ks/index-page-body)))

(facts "A signed in user can sign out"
       (-> (k/session (clean-app!))
           sign-in!
           (k/visit (routes/absolute-path (c/create-config) :index))
           (kh/check-and-follow ks/header-sign-out-link)
           (kh/check-and-follow-redirect "to sign-in page after signing out")
           (kh/page-uri-is "/sign-in")
           (kh/response-status-is 200)
           (kh/selector-not-present ks/header-sign-out-link)))

(def server (atom nil))
(defn start-server [] (swap! server (fn [_] (ring-jetty/run-jetty
                                              (h/create-app (c/create-config) database {})
                                              {:host "127.0.0.1" :port 3000 :join? false}))))
(defn stop-server [] (.stop @server))

(against-background
  [(before :contents (start-server))
   (after :contents (stop-server))]
  (facts "Stub activities are rendered"
         (drop-db!)
         (-> (k/session (h/create-app (c/create-config) database {:stub-activity-source "http://127.0.0.1:3000/stub-activities"}))
             sign-in!
             (k/visit (routes/path :index))
             (kh/page-uri-is "/")
             (kh/response-status-is 200)
             (kh/selector-exists ks/index-page-body)
             (kh/selector-includes-content ks/index-page-activity-item-title "Stub activity title")
             (kh/selector-includes-content ks/index-page-activity-item-action "Barry - STUB_ACTIVITY - Create")
             (kh/selector-has-attribute-with-content ks/index-page-activity-item-link :href "http://stub-activity.url")
             )))


(facts "Invalid activity source responses are handled gracefully"
       (drop-db!)
       (-> (k/session (h/create-app (c/create-config) database {:invalid-activity-src "http://localhost:6666/not-an-activity-source"}))
           sign-in!
           (k/visit (routes/absolute-path (c/create-config) :index))
           (kh/page-uri-is "/")
           (kh/response-status-is 200)))

(facts "Error page is shown if an exception is thrown"
       (against-background
         (h/sign-in anything) =throws=> (Exception.))
       (-> (k/session (h/create-app (c/create-config) database {}))
           (k/visit (routes/absolute-path (c/create-config) :sign-in))
           (kh/response-status-is 500)
           (kh/selector-exists ks/error-500-page-body)))

(facts "Going to an unknown uri renders the 404 page"
       (-> (k/session app)
           (k/visit "http://localhost:3000/not-a-valid-uri")
           (kh/page-uri-is "/not-a-valid-uri")
           (kh/response-status-is 404)
           (kh/selector-exists ks/error-404-page-body)))
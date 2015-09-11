(ns mooncake.integration.kerodon
  (:require [monger.core :as monger]
            [monger.db :as mdb]
            [midje.sweet :refer :all]
            [kerodon.core :as k]
            [net.cgrand.enlive-html :as html]
            [ring.adapter.jetty :as ring-jetty]
            [stonecutter-oauth.client :as soc]
            [stonecutter-oauth.jwt :as so-jwt]
            [ring.util.response :as r]
            [mooncake.routes :as routes]
            [mooncake.activity :as a]
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
  (soc/request-access-token! anything anything) => {:id_token ...id-token...}
  (so-jwt/get-public-key-string-from-jwk-set-url anything) => ...public-key-str...
  (so-jwt/decode anything ...id-token... ...public-key-str...) => {:sub "test-stonecutter-user-uuid"})

(def test-db-uri "mongodb://localhost:27017/mooncake")
(def database (mongo/create-database (mongo/get-mongo-db test-db-uri)))

(def app (h/create-app (c/create-config) database {}))

(def app-with-activity-sources-from-yaml (h/create-app (c/create-config) database (a/load-activity-sources "test-activity-sources.yml")))

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
      (kh/check-page-is "/create-account" ks/create-account-page-body)
      (kh/check-and-fill-in ks/create-account-page-username-input "Barry")
      (kh/check-and-press ks/create-account-page-submit-button)))

(facts "The feed page redirects to /sign-in when user is not signed in"
       (-> (k/session app)
           (k/visit (routes/absolute-path (c/create-config) :feed))
           (kh/check-and-follow-redirect "sign-in")
           (kh/check-page-is "/sign-in" ks/sign-in-page-body)))

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
           (kh/check-page-is "/create-account" ks/create-account-page-body)
           (kh/check-and-fill-in ks/create-account-page-username-input "Barry")
           (kh/check-and-press ks/create-account-page-submit-button)
           (kh/check-and-follow-redirect "to /")
           (kh/check-page-is "/" ks/feed-page-body)))

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
           (kh/check-page-is "/" ks/feed-page-body)))

(facts "A signed in user can sign out"
       (-> (k/session (clean-app!))
           sign-in!
           (k/visit (routes/absolute-path (c/create-config) :feed))
           (kh/check-and-follow ks/header-sign-out-link)
           (kh/check-and-follow-redirect "to sign-in page after signing out")
           (kh/check-page-is "/sign-in" ks/sign-in-page-body)
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
         (-> (k/session (h/create-app (c/create-config) database {:stub-activity-source {:url "http://127.0.0.1:3000/stub-activities"}}))
             sign-in!
             (k/visit (routes/path :feed))
             (kh/check-page-is "/" ks/feed-page-body)
             (kh/selector-includes-content ks/feed-page-activity-item-title "Stub activity title")
             (kh/selector-includes-content ks/feed-page-activity-item-action "Barry - STUB_ACTIVITY - Create")
             (kh/selector-has-attribute-with-content ks/feed-page-activity-item-link :href "http://stub-activity.url"))))

(facts "User can see current feed preferences"
       (drop-db!)
       (-> (k/session app-with-activity-sources-from-yaml)
           sign-in!
           (k/visit (routes/path :show-customise-feed))
           (kh/check-page-is "/customise-feed" ks/customise-feed-page-body)
           (kh/selector-includes-content [ks/customise-feed-page-feed-item-list-item-label (html/attr= :for "test-activity-source-1")] "Test Activity Source 1")
           (kh/selector-includes-content [ks/customise-feed-page-feed-item-list-item-label (html/attr= :for "test-activity-source-2")] "Test Activity Source 2")
           (kh/selector-has-attribute-with-content [ks/customise-feed-page-feed-item-checkbox (html/attr= :id "test-activity-source-1")] :checked "checked")
           (kh/selector-has-attribute-with-content [ks/customise-feed-page-feed-item-checkbox (html/attr= :id "test-activity-source-2")] :checked "checked")))

(facts "User can customise feed preferences"
       (drop-db!)
       (-> (k/session app-with-activity-sources-from-yaml)
           sign-in!
           (k/visit (routes/path :show-customise-feed))
           (kh/check-page-is "/customise-feed" ks/customise-feed-page-body)
           (k/uncheck [ks/customise-feed-page-feed-item-checkbox (html/attr= :id "test-activity-source-1")])
           (kh/check-and-press ks/customise-feed-page-submit-button)
           (kh/check-and-follow-redirect "to /")
           (kh/check-page-is "/" ks/feed-page-body)
           (k/visit (routes/path :show-customise-feed))
           (kh/check-page-is "/customise-feed" ks/customise-feed-page-body)
           (kh/selector-does-not-have-attribute [ks/customise-feed-page-feed-item-checkbox (html/attr= :id "test-activity-source-1")] :checked)
           (kh/selector-has-attribute-with-content [ks/customise-feed-page-feed-item-checkbox (html/attr= :id "test-activity-source-2")] :checked "checked")))

(facts "User can sign out from the customise feed page"
       (drop-db!)
       (-> (k/session app-with-activity-sources-from-yaml)
           sign-in!
           (k/visit (routes/path :show-customise-feed))
           (kh/check-page-is "/customise-feed" ks/customise-feed-page-body)
           (kh/check-and-follow ks/header-sign-out-link)
           (kh/check-and-follow-redirect "to sign-in page after signing out")
           (kh/check-page-is "/sign-in" ks/sign-in-page-body)
           (kh/selector-not-present ks/header-sign-out-link)))

(facts "Invalid activity source responses are handled gracefully"
       (drop-db!)
       (-> (k/session (h/create-app (c/create-config) database {:invalid-activity-src {:url "http://localhost:6666/not-an-activity-source"}}))
           sign-in!
           (k/visit (routes/absolute-path (c/create-config) :feed))
           (kh/check-page-is "/" ks/feed-page-body)))

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
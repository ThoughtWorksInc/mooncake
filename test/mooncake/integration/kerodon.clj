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
            [mooncake.db.mongo :as mongo]
            [mooncake.db.activity :as adb]))

(def ten-oclock "2015-01-01T10:00:00.000Z")
(def eleven-oclock "2015-01-01T11:00:00.000Z")

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

(defn clean-app-with-activity-sources-from-yaml-and-activity-types-populated-in-db []
  (drop-db!)
  (adb/update-activity-types-for-activity-source! database "test-activity-source-1" "TestActivityType-1-1")
  (adb/update-activity-types-for-activity-source! database "test-activity-source-1" "TestActivityType-1-2")
  (adb/update-activity-types-for-activity-source! database "test-activity-source-2" "TestActivityType-2-1")
  (adb/update-activity-types-for-activity-source! database "test-activity-source-3" "Question")
  (adb/update-activity-types-for-activity-source! database "test-activity-source-3" "Create")
  app-with-activity-sources-from-yaml)

(defn populate-db-with-stub-activities! [activities]
  (doseq [activity activities]
    (adb/update-activity-types-for-activity-source! database (get activity "activity-src") (get activity "@type"))
    (mongo/store! database adb/activity-collection activity)))

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

(defn page-contains-feed-item [state position title author action link]
  (-> state
      (kh/selector-includes-content ks/feed-page-activity-item-title title position)
      (kh/selector-includes-content ks/feed-page-activity-item-author author position)
      (kh/selector-includes-content ks/feed-page-activity-item-action action position)
      (kh/selector-has-attribute-with-content ks/feed-page-activity-item-link :href link position))
  state)

(against-background
  [(before :contents (start-server))
   (after :contents (stop-server))]
  (facts "Stub activities are rendered"
         (drop-db!)
         (-> (k/session (h/create-app (c/create-config) database {:stub-activity-source {:url "http://127.0.0.1:3000/stub-activities"}}))
             sign-in!
             (k/visit (routes/path :feed))
             (kh/check-page-is "/" ks/feed-page-body)
             (page-contains-feed-item first "Stub activity title" "Barry" "- STUB_ACTIVITY - Create" "http://stub-activity.url")
             (page-contains-feed-item second "Objective title" "John Doe" "created an objective" "http://objective-activity.url")
             (page-contains-feed-item #(nth % 2) "Question title" "Jane Q. Public" "asked a question" "http://question-activity.url"))))

(facts "User can see current feed preferences"
       (-> (k/session (clean-app-with-activity-sources-from-yaml-and-activity-types-populated-in-db))
           sign-in!
           (k/visit (routes/path :show-customise-feed))
           (kh/check-page-is "/customise-feed" ks/customise-feed-page-body)
           (kh/selector-includes-content [ks/customise-feed-page-feed-item-child-list-item-label
                                          (html/attr= :for "test-activity-source-1_-_TestActivityType-1-1")] "TestActivityType-1-1")
           (kh/selector-includes-content [ks/customise-feed-page-feed-item-child-list-item-label
                                          (html/attr= :for "test-activity-source-1_-_TestActivityType-1-2")] "TestActivityType-1-2")
           (kh/selector-includes-content [ks/customise-feed-page-feed-item-child-list-item-label
                                          (html/attr= :for "test-activity-source-2_-_TestActivityType-2-1")] "TestActivityType-2-1")
           (kh/selector-has-attribute-with-content [ks/customise-feed-page-feed-item-child-checkbox
                                                    (html/attr= :id "test-activity-source-1_-_TestActivityType-1-1")] :checked "checked")
           (kh/selector-has-attribute-with-content [ks/customise-feed-page-feed-item-child-checkbox
                                                    (html/attr= :id "test-activity-source-1_-_TestActivityType-1-2")] :checked "checked")
           (kh/selector-has-attribute-with-content [ks/customise-feed-page-feed-item-child-checkbox
                                                    (html/attr= :id "test-activity-source-2_-_TestActivityType-2-1")] :checked "checked")
           (kh/selector-exists [ks/customise-feed-page-feed-item-child-checkbox
                                (html/attr= :id "test-activity-source-2_-_TestActivityType-2-1")])))


(facts "User can customise feed preferences - changes to activity types for feeds are reflected on the 'customise feed' form"
              (-> (k/session (clean-app-with-activity-sources-from-yaml-and-activity-types-populated-in-db))
                  sign-in!
                  (k/visit (routes/path :show-customise-feed))
                  (kh/check-page-is "/customise-feed" ks/customise-feed-page-body)
                  (k/uncheck [ks/customise-feed-page-feed-item-child-checkbox (html/attr= :id "test-activity-source-1_-_TestActivityType-1-1")])
                  (k/uncheck [ks/customise-feed-page-feed-item-child-checkbox (html/attr= :id "test-activity-source-2_-_TestActivityType-2-1")])
                  (kh/check-and-press ks/customise-feed-page-submit-button)
                  (kh/check-and-follow-redirect "to /")
                  (kh/check-page-is "/" ks/feed-page-body)
                  (k/visit (routes/path :show-customise-feed))
                  (kh/check-page-is "/customise-feed" ks/customise-feed-page-body)
                  (kh/selector-does-not-have-attribute [ks/customise-feed-page-feed-item-child-checkbox (html/attr= :id "test-activity-source-1_-_TestActivityType-1-1")] :checked)
                  (kh/selector-has-attribute-with-content [ks/customise-feed-page-feed-item-child-checkbox (html/attr= :id "test-activity-source-1_-_TestActivityType-1-2")] :checked "checked")
                  (kh/selector-does-not-have-attribute [ks/customise-feed-page-feed-item-child-checkbox (html/attr= :id "test-activity-source-2_-_TestActivityType-2-1")] :checked)))

(facts "User can customise feed preferences - activities of disabled types are not shown on the 'feed' page"
              (drop-db!)
              (populate-db-with-stub-activities! [{"object"       {"displayName" "Activity 1 Title"}
                                                   "published"    ten-oclock
                                                   "activity-src" "test-activity-source-1"
                                                   "@type"        "TestActivityType-1-1"}
                                                  {"object"       {"displayName" "Activity 2 Title"}
                                                   "published"    ten-oclock
                                                   "activity-src" "test-activity-source-1"
                                                   "@type"        "TestActivityType-1-2"}
                                                  {"object"       {"displayName" "Activity 3 Title"}
                                                   "published"    eleven-oclock
                                                   "activity-src" "test-activity-source-2"
                                                   "@type"        "TestActivityType-2-1"}])

              (-> (k/session app-with-activity-sources-from-yaml)
                  sign-in!
                  (k/visit (routes/path :feed))
                  (kh/check-page-is "/" ks/feed-page-body)
                  (kh/selector-includes-content ks/feed-page-activity-list "Activity 1 Title")
                  (kh/selector-includes-content ks/feed-page-activity-list "Activity 2 Title")
                  (kh/selector-includes-content ks/feed-page-activity-list "Activity 3 Title")
                  (kh/check-and-follow ks/header-customise-feed-link)
                  (kh/check-page-is "/customise-feed" ks/customise-feed-page-body)
                  (k/uncheck [ks/customise-feed-page-feed-item-child-checkbox (html/attr= :id "test-activity-source-1_-_TestActivityType-1-1")])
                  (k/uncheck [ks/customise-feed-page-feed-item-child-checkbox (html/attr= :id "test-activity-source-2_-_TestActivityType-2-1")])
                  (kh/check-and-press ks/customise-feed-page-submit-button)
                  (kh/check-and-follow-redirect "to /")
                  (kh/check-page-is "/" ks/feed-page-body)
                  (kh/selector-includes-content ks/feed-page-activity-list "Activity 2 Title")
                  (kh/selector-does-not-include-content ks/feed-page-activity-list "Activity 1 Title")
                  (kh/selector-does-not-include-content ks/feed-page-activity-list "Activity 3 Title")))

(facts "A message is displayed on feed page if user disables all activity types"
              (drop-db!)
              (populate-db-with-stub-activities! [{"object"       {"displayName" "Activity 1 Title"}
                                                   "published"    ten-oclock
                                                   "activity-src" "test-activity-source-1"
                                                   "@type"        "TestActivityType-1-1"}])
              (-> (k/session app-with-activity-sources-from-yaml)
                  sign-in!
                  (k/visit (routes/path :show-customise-feed))
                  (kh/check-page-is "/customise-feed" ks/customise-feed-page-body)
                  (k/uncheck [ks/customise-feed-page-feed-item-child-checkbox (html/attr= :id "test-activity-source-1_-_TestActivityType-1-1")])
                  (kh/check-and-press ks/customise-feed-page-submit-button)
                  (kh/check-and-follow-redirect "to /")
                  (kh/check-page-is "/" ks/feed-page-body)
                  (kh/selector-exists ks/feed-page-no-active-sources-message)))

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

(facts "Displayed content length of activities is limited to 140 characters"
       (drop-db!)
       (populate-db-with-stub-activities! [{"@type"        "Create"
                                            "object"       {"@type"       "Objective"
                                                            "displayName" (str "Lorem ipsum dolor sit amet, consectetur "
                                                                               "adipiscing elit. Morbi nunc tortor, eleifend et egestas sit "
                                                                               "amet, tincidunt ac augue. Mauris pellentesque sed.")}
                                            "actor"        {"displayName" "John Doe"}
                                            "published"    eleven-oclock
                                            "activity-src" "test-activity-source-3"}
                                           {"@type"        "Question"
                                            "object"       {"@type"       "Objective Question"
                                                            "displayName" (str "Nullam fermentum, magna et pellentesque "
                                                                               "ultrices, libero arcu elementum diam, id molestie urna velit "
                                                                               "ultrices quam. Mauris id commodo nequeamat. Fusce posuere.")}
                                            "actor"        {"displayName" "Jane Q Public"}
                                            "published"    ten-oclock
                                            "activity-src" "test-activity-source-3"}])
       (let [expected-objective-title (str "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Morbi nunc tortor, "
                                           "eleifend et egestas sit amet, tincidunt ac augue. Mauris\u2026")
             expected-question-title (str "Nullam fermentum, magna et pellentesque ultrices, libero arcu elementum diam, "
                                          "id molestie urna velit ultrices quam. Mauris id commodo\u2026")]
         (-> (k/session app-with-activity-sources-from-yaml)
             sign-in!
             (k/visit (routes/path :feed))
             (kh/check-page-is "/" ks/feed-page-body)
             (page-contains-feed-item first expected-objective-title "John Doe" "created an objective" "")
             (page-contains-feed-item second expected-question-title "Jane Q Public" "asked a question" ""))))
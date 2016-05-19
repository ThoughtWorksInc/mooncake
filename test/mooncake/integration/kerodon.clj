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
            [mooncake.db.activity :as adb]
            [clj-time.format :as f]
            [clj-time.core :as t]
            [mooncake.config :as config])
  (:import (org.bson.types ObjectId)))

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
(def mongo-store (mongo/create-mongo-store (mongo/get-mongo-db test-db-uri)))

(def app (h/create-site-app (c/create-config) mongo-store {}))

(def app-with-activity-sources-from-yaml (h/create-site-app (c/create-config) mongo-store (a/load-activity-sources-from-resource "test-activity-sources.yml")))

(defn drop-db! []
  (let [{:keys [conn db]} (monger/connect-via-uri test-db-uri)]
    (mdb/drop-db db)
    (monger/disconnect conn)))

(defn clean-app-with-activity-sources-from-yaml-and-activity-types-populated-in-db []
  (drop-db!)
  (adb/update-activity-types-for-activity-source! mongo-store "test-activity-source-1" "TestActivityType-1-1")
  (adb/update-activity-types-for-activity-source! mongo-store "test-activity-source-1" "TestActivityType-1-2")
  (adb/update-activity-types-for-activity-source! mongo-store "test-activity-source-2" "TestActivityType-2-1")
  (adb/update-activity-types-for-activity-source! mongo-store "test-activity-source-3" "Question")
  (adb/update-activity-types-for-activity-source! mongo-store "test-activity-source-3" "Create")
  app-with-activity-sources-from-yaml)

(defn populate-db-with-stub-activities! [store activities]
  (doseq [activity activities]
    (adb/update-activity-types-for-activity-source! store (get activity :activity-src) (get activity :type))
    (mongo/store! store adb/activity-collection activity)))

; NOTE 16/10/2015 CW: similar fn in test_helpers/db
(defn create-dummy-activities [store amount]
  (->> (range amount)
       (map (fn [counter]
              {:actor         {:displayName (str "TestData" counter)}
               :published     (f/unparse (f/formatters :date-time) (t/plus (t/date-time 2015 8 12) (t/seconds counter)))
               :activity-src  "test-activity-source-1"
               :type          "Create"
               :relInsertTime (ObjectId.)}))
       (populate-db-with-stub-activities! store)))

(defn create-dummy-activity [store timestamp]
  (->> [{:actor         {:displayName (str "Single Activity")}
         :published     timestamp
         :activity-src  "test-activity-source-1"
         :type          "Create"
         :relInsertTime (ObjectId.)}]
       (populate-db-with-stub-activities! store)))

(defn clean-app! []
  (drop-db!)
  app)

(defn authenticate-against-stub [state]
  (k/visit state (str (routes/absolute-path (c/create-config) :stonecutter-callback) "?code=1234")))

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
         (r/redirect (str (routes/absolute-path (c/create-config) :stonecutter-callback) "?code=1234")))
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

(facts "If user authentication fails then user is returned to sign in page with flash message"
       (against-background
         (soc/authorisation-redirect-response anything) =>
         (r/redirect (str (routes/absolute-path (c/create-config) :stonecutter-callback) "?error=access_denied")))
       (-> (k/session (clean-app!))
           (k/visit (routes/absolute-path (c/create-config) :sign-in))
           (kh/check-and-follow ks/sign-in-page-sign-in-with-d-cent-link)
           (kh/check-and-follow-redirect "to stonecutter")
           (kh/check-and-follow-redirect "to sign-in page")
           (kh/check-page-is "/sign-in" ks/sign-in-page-body)
           (kh/selector-exists ks/sign-in-flash-message)))

(facts "An existing user is redirected to / (rather than /create-account) after authenticating with stonecutter"
       (against-background
         (soc/authorisation-redirect-response anything) =>
         (r/redirect (str (routes/absolute-path (c/create-config) :stonecutter-callback) "?code=1234")))
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
                                              (h/create-site-app (c/create-config) mongo-store {})
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
  (facts "Stub activities from the unsigned source are rendered"
         (drop-db!)
         (-> (k/session (h/create-site-app (c/create-config) mongo-store {:stub-activity-source {:url "http://127.0.0.1:3000/stub-activities"}}))
             sign-in!
             (k/visit (routes/path :feed))
             (kh/check-page-is "/" ks/feed-page-body)
             (page-contains-feed-item first "prendiamo come esempio la legge svedese? o altre leggi simili nel mondo?" "fabrisspec" "asked a question" "http://objective8.dcentproject.eu/objectives/20/questions/21")
             (page-contains-feed-item second "CONNETTIVITA' GRATUITA" "pandeussilvae" "created an objective" "http://objective8.dcentproject.eu/objectives/20")
             (page-contains-feed-item #(nth % 2) "What is this all about?" "Jc" "asked a question" "http://objective8.dcentproject.eu/objectives/19/questions/20")
             (kh/selector-exists ks/untrusted-source-warning-icon)
             (kh/selector-not-present ks/unverified-signature-warning-icon))))

(against-background
  [(before :contents (start-server))
   (after :contents (stop-server))]
  (facts "Stub activities from the signed source are rendered"
         (drop-db!)
         (-> (k/session (h/create-site-app (c/create-config) mongo-store {:stub-activity-source {:url "http://127.0.0.1:3000/stub-signed-activities"}}))
             sign-in!
             (k/visit (routes/path :feed))
             (kh/check-page-is "/" ks/feed-page-body)
             (page-contains-feed-item first "prendiamo come esempio la legge svedese? o altre leggi simili nel mondo?" "fabrisspec" "asked a question" "http://objective8.dcentproject.eu/objectives/20/questions/21")
             (page-contains-feed-item second "CONNETTIVITA' GRATUITA" "pandeussilvae" "created an objective" "http://objective8.dcentproject.eu/objectives/20")
             (page-contains-feed-item #(nth % 2) "What is this all about?" "Jc" "asked a question" "http://objective8.dcentproject.eu/objectives/19/questions/20")
             (kh/selector-not-present ks/untrusted-source-warning-icon)
             (kh/selector-not-present ks/unverified-signature-warning-icon))))

(against-background
  [(before :contents (start-server))
   (after :contents (stop-server))]
  (facts "Stub activities with unverified signature from the signed source are rendered"
         (drop-db!)
         (-> (k/session (h/create-site-app (c/create-config) mongo-store {:stub-activity-source {:url "http://127.0.0.1:3000/stub-signed-activities-verification-failure"}}))
             sign-in!
             (k/visit (routes/path :feed))
             (kh/check-page-is "/" ks/feed-page-body)
             (page-contains-feed-item first "prendiamo come esempio la legge svedese? o altre leggi simili nel mondo?" "fabrisspec" "asked a question" "http://objective8.dcentproject.eu/objectives/20/questions/21")
             (page-contains-feed-item second "CONNETTIVITA' GRATUITA" "pandeussilvae" "created an objective" "http://objective8.dcentproject.eu/objectives/20")
             (page-contains-feed-item #(nth % 2) "What is this all about?" "Jc" "asked a question" "http://objective8.dcentproject.eu/objectives/19/questions/20")
             (kh/selector-not-present ks/untrusted-source-warning-icon)
             (kh/selector-exists ks/unverified-signature-warning-icon))))


(facts "User can see current feed preferences"
       (-> (k/session (clean-app-with-activity-sources-from-yaml-and-activity-types-populated-in-db))
           sign-in!
           (k/visit (routes/path :show-customise-feed))
           (kh/check-page-is "/customise-feed" ks/customise-feed-page-body)
           (kh/selector-has-attribute-with-content [ks/customise-feed-page-feed-item-child-list-item-name] :data-l8n "content:activity-type/customise-feed-testactivitytype-1-1")
           (kh/selector-has-attribute-with-content [ks/customise-feed-page-feed-item-child-list-item-name] :data-l8n "content:activity-type/customise-feed-testactivitytype-1-2" second)
           (kh/selector-has-attribute-with-content [ks/customise-feed-page-feed-item-child-list-item-name] :data-l8n "content:activity-type/customise-feed-testactivitytype-2-1" #(nth % 2))
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
       (populate-db-with-stub-activities! mongo-store [{:object       {:displayName "Activity 1 Title"}
                                                        :published    ten-oclock
                                                        :activity-src "test-activity-source-1"
                                                        :type         "TestActivityType-1-1"}
                                                       {:object       {:displayName "Activity 2 Title"}
                                                        :published    ten-oclock
                                                        :activity-src "test-activity-source-1"
                                                        :type         "TestActivityType-1-2"}
                                                       {:object       {:displayName "Activity 3 Title"}
                                                        :published    eleven-oclock
                                                        :activity-src "test-activity-source-2"
                                                        :type         "TestActivityType-2-1"}])

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
       (populate-db-with-stub-activities! mongo-store [{:object       {:displayName "Activity 1 Title"}
                                                        :published    ten-oclock
                                                        :activity-src "test-activity-source-1"
                                                        :type         "TestActivityType-1-1"}])
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
       (-> (k/session (h/create-site-app (c/create-config) mongo-store {:invalid-activity-src {:url "http://localhost:6666/not-an-activity-source"}}))
           sign-in!
           (k/visit (routes/absolute-path (c/create-config) :feed))
           (kh/check-page-is "/" ks/feed-page-body)))

(facts "Error page is shown if an exception is thrown"
       (against-background
         (h/sign-in anything) =throws=> (Exception.))
       (-> (k/session (h/create-site-app (c/create-config) mongo-store {}))
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
       (populate-db-with-stub-activities! mongo-store [{:type         "Create"
                                                        :object       {:type        "Objective"
                                                                       :displayName (str "Lorem ipsum dolor sit amet, consectetur "
                                                                                         "adipiscing elit. Morbi nunc tortor, eleifend et egestas sit "
                                                                                         "amet, tincidunt ac augue. Mauris pellentesque sed.")}
                                                        :actor        {:displayName "John Doe"}
                                                        :published    eleven-oclock
                                                        :activity-src "test-activity-source-3"}
                                                       {:type         "Question"
                                                        :object       {:type        "Objective Question"
                                                                       :displayName (str "Nullam fermentum, magna et pellentesque "
                                                                                         "ultrices, libero arcu elementum diam, id molestie urna velit "
                                                                                         "ultrices quam. Mauris id commodo nequeamat. Fusce posuere.")}
                                                        :actor        {:displayName "Jane Q Public"}
                                                        :published    ten-oclock
                                                        :activity-src "test-activity-source-3"}])
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

(facts "Newer and older links are hidden or shown based on page numbers"
       (fact "Links aren't shown when only 1 page exists"
             (drop-db!)
             (create-dummy-activities mongo-store 1)
             (-> (k/session app-with-activity-sources-from-yaml)
                 sign-in!
                 (k/visit (routes/path :feed))
                 (kh/check-page-is "/" ks/feed-page-body)
                 (kh/selector-not-present ks/newer-activities-link)
                 (kh/selector-not-present ks/older-activities-link)))
       (fact "Multiple pages"
             (drop-db!)
             (create-dummy-activities mongo-store (+ 1 (* 2 config/activities-per-page)))
             (-> (k/session app-with-activity-sources-from-yaml)
                 sign-in!
                 (k/visit (routes/path :feed))
                 (kh/check-page-is "/" ks/feed-page-body)
                 (kh/selector-not-present ks/newer-activities-link)
                 (kh/selector-exists ks/older-activities-link)

                 (kh/check-and-follow ks/older-activities-link)
                 (kh/check-page-is "/" ks/feed-page-body)
                 (kh/params-contains :query-string "page-number=2")
                 (kh/selector-exists ks/newer-activities-link)
                 (kh/selector-exists ks/older-activities-link)

                 (kh/check-and-follow ks/older-activities-link)
                 (kh/check-page-is "/" ks/feed-page-body)
                 (kh/params-contains :query-string "page-number=3")
                 (kh/selector-exists ks/newer-activities-link)
                 (kh/selector-not-present ks/older-activities-link)
                 (kh/page-contains-amount-of-activities 1)

                 (kh/check-and-follow ks/newer-activities-link)
                 (kh/check-and-follow ks/newer-activities-link)
                 (kh/check-page-is "/" ks/feed-page-body)
                 (kh/params-contains :query-string "page-number=1"))))


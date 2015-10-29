(ns mooncake.test.controller.customise-feed
  (:require [midje.sweet :refer :all]
            [net.cgrand.enlive-html :as html]
            [mooncake.controller.customise-feed :as cf]
            [mooncake.db.user :as user]
            [mooncake.routes :as routes]
            [mooncake.test.test-helpers.enlive :as eh]
            [mooncake.test.test-helpers.db :as dbh]))

(facts "about customise-feed"
       (let [customise-feed-request {:context {:activity-sources {:activity-src         {:name           "A. Activity Source"
                                                                                         :url            "some url"
                                                                                         :activity-types ["Create" "Question"]}
                                                                  :another-activity-src {:name           "B. Another Source"
                                                                                         :url            "another url"
                                                                                         :activity-types ["Question"]}}}
                                     :params  {:activity-src_-_Question "anything"
                                               :some-other-param        "something-else"}
                                     :session {:username ...username...}}
             store (dbh/create-in-memory-store)
             stored-user (user/create-user! store ...user-id... ...username...)
             response (cf/customise-feed store customise-feed-request)]
         (fact "it should update the user's feed settings for all activity sources and their types"

               (user/find-user store ...username...) => {:auth-provider-user-id ...user-id...
                                                      :username              ...username...
                                                      :feed-settings         {:activity-src         {:types    [{:id       "Create"
                                                                                                                 :selected false}
                                                                                                                {:id       "Question"
                                                                                                                 :selected true}]}
                                                                              :another-activity-src {:types    [{:id       "Question"
                                                                                                                 :selected false}]}}})
         (fact "it should redirect to /"
               response => (eh/check-redirects-to (routes/absolute-path {} :feed)))))

(facts "about show-customise-feed"
       (let [show-customise-feed-request {:context {:activity-sources {:activity-src             {:name           "A. Activity Source"
                                                                                                  :url            "some url"
                                                                                                  :activity-types ["Create" "Question"]}
                                                                       :another-activity-src     {:name           "B. Another Source"
                                                                                                  :url            "another url"
                                                                                                  :activity-types ["Add"]}
                                                                       :yet-another-activity-src {:name           "C. Yet Another Source"
                                                                                                  :url            "yet another url"
                                                                                                  :activity-types ["Add"]}}}
                                          :t {}
                                          :session {:username ...username...}}
             store (dbh/create-in-memory-store)
             _ (user/create-user! store ...user-id... ...username...)
             _ (user/update-feed-settings! store ...username... {:activity-src         {:types    [{:id       "Create"
                                                                                                 :selected false}
                                                                                                {:id       "Question"
                                                                                                 :selected true}]}
                                                              :another-activity-src {:types    [{:id       "Add"
                                                                                                 :selected false}]}})
             response (cf/show-customise-feed store show-customise-feed-request)]
         (fact "it should show available activity sources on feed settings page"
               response => (eh/check-renders-page [:.func--customise-feed-page])
               (:body response) => (contains "Activity Source")
               (:body response) => (contains "Another Source"))

         (fact "it should show available activity types for available sources on feed settings page"
               (:body response) => (contains "Create")
               (:body response) => (contains "Question")
               (:body response) => (contains "Add"))

         (fact "it should select activity types from activity sources based on user's feed settings"

               (let [[first-child-checkbox second-child-checkbox third-child-checkbox] (-> (html/html-snippet (:body response))
                                                                                           (html/select [:.clj--feed-item-child__checkbox]))]

                 (-> first-child-checkbox :attrs :id) => "activity-src_-_Create"
                 (-> first-child-checkbox :attrs :checked) => nil

                 (-> second-child-checkbox :attrs :id) => "activity-src_-_Question"
                 (-> second-child-checkbox :attrs :checked) => "checked"

                 (-> third-child-checkbox :attrs :id) => "another-activity-src_-_Add"
                 (-> third-child-checkbox :attrs :checked) => nil))

         (fact "it should select activity types which are not explicitly set in user preferences by default"
               (let [_ (user/update-feed-settings! store ...username... {:activity-src         {:types    [{:id       "Question"
                                                                                                         :selected false}]}})
                     response (cf/show-customise-feed store show-customise-feed-request)
                     [first-child-checkbox second-child-checkbox third-child-checkbox fourth-child-checkbox] (-> (html/html-snippet (:body response))
                                                                                                                 (html/select [:.clj--feed-item-child__checkbox]))]

                 (-> first-child-checkbox :attrs :id) => "activity-src_-_Create"
                 (-> first-child-checkbox :attrs :checked) => "checked"

                 (-> second-child-checkbox :attrs :id) => "activity-src_-_Question"
                 (-> second-child-checkbox :attrs :checked) => nil

                 (-> third-child-checkbox :attrs :id) => "another-activity-src_-_Add"
                 (-> third-child-checkbox :attrs :checked) => "checked"

                 (-> fourth-child-checkbox :attrs :id) => "yet-another-activity-src_-_Add"
                 (-> fourth-child-checkbox :attrs :checked) => "checked"))))

(facts "about generate-activity-source-preferences with no user preferences"
       (let [activity-sources {:activity-src             {:name "C. Activity Source"
                                                          :url  "some url"
                                                          :activity-types ["Type1" "Type2"]}
                               :another-activity-src     {:name "A. Another Source"
                                                          :url  "another url"
                                                          :activity-types ["Type1"]}
                               :yet-another-activity-src {:name "B. Yet Another Source"
                                                          :url  "yet another url"
                                                          :activity-types ["Type2"]}}
             non-existent-user-feed-preferences nil
             result (cf/generate-activity-source-preferences activity-sources non-existent-user-feed-preferences)]

         (fact "they include activity source ids for all sources"
               (map :id result) => (contains ["activity-src" "another-activity-src" "yet-another-activity-src"] :in-any-order))
         (fact "they include activity source name for all sources"
               (map :name result) => (contains ["C. Activity Source" "A. Another Source" "B. Yet Another Source"] :in-any-order))
         (fact "they include activity source url for all sources"
               (map :url result) => (contains ["some url" "another url" "yet another url"] :in-any-order))
         (fact "they are sorted by name"
               (map :name result) => ["A. Another Source" "B. Yet Another Source" "C. Activity Source"])
         (fact "activity types are selected by default"
               result => [{:name "A. Another Source" :id "another-activity-src" :url "another url" :signed? true :activity-types [{:id "Type1" :selected true}]}
                          {:name "B. Yet Another Source" :id "yet-another-activity-src" :url "yet another url" :signed? true :activity-types [{:id "Type2" :selected true}]}
                          {:name "C. Activity Source" :id "activity-src" :url "some url" :signed? true :activity-types [{:id "Type1" :selected true}
                                                                                                          {:id "Type2" :selected true}]}])))

(facts "about generate-activity-type-preferences"
       (let [available-activity-types-from-source ["Create" "Add" "Question"]
             user-feed-activity-type-settings [{:id       "Create"
                                                :selected false}
                                               {:id       "Question"
                                                :selected true}]
             result (cf/generate-activity-type-preferences available-activity-types-from-source user-feed-activity-type-settings)]
         (fact "activity types missing from user preferences are selected by default"
               result => (contains {:id "Add"
                                    :selected true}))
         (fact "they include activity type IDs for all activity types"
               (map :id result) => (contains ["Create" "Add" "Question"] :in-any-order))
         (fact "they are sorted alphabetically by activity type ID"
               (map :id result) => ["Add" "Create" "Question"])
         (fact "they include 'selected' flag"
               (map :selected result) => [true false true])))

(tabular
  (fact "about selected-feed? - true if not set"
        (cf/selected-feed-type? ?feed-setting-input) => ?feed-setting-output)
  ?feed-setting-input    ?feed-setting-output
  true                   true
  false                  false
  nil                    true
  ...anything-else...    true)

(facts "about user-preference-for-activity-type"
       (let [user-feed-activity-type-settings [{:id       "Create"
                                                :selected false}
                                               {:id       "Question"
                                                :selected true}]]
         (fact "returns user-preference for the given Id"
               (cf/user-preference-for-activity-type user-feed-activity-type-settings "Create") => false
               (cf/user-preference-for-activity-type user-feed-activity-type-settings "Question") => true)
         (fact "returns true if activity type is not already in user preferences"
               (cf/user-preference-for-activity-type user-feed-activity-type-settings "Add") => true)))

(facts "about create-user-feed-settings-for-source"
       (let [single-activity-source-configuration {:name           "A. Activity Source"
                                                   :url            "some url"
                                                   :activity-types '("Create" "Question")}]
         (fact "unselects all if no parameters submitted"
               (let [submitted-parameters {}
                     expected-user-feed-settings {:types    [{:id       "Create"
                                                              :selected false}
                                                             {:id       "Question"
                                                              :selected false}]}]
                 (cf/create-user-feed-settings-for-source :activity-src single-activity-source-configuration submitted-parameters) => expected-user-feed-settings))
         (fact "selects activity types which match submitted parameters"
               (let [submitted-parameters {:activity-src_-_Create "bar"}
                     expected-user-feed-settings {:types    [{:id       "Create"
                                                              :selected true}
                                                             {:id       "Question"
                                                              :selected false}]}]
                 (cf/create-user-feed-settings-for-source :activity-src single-activity-source-configuration submitted-parameters) => expected-user-feed-settings))))

(facts "about signed activitiy sources"
       (fact "generate activity source should return information on whether the source is signed or not"
             (against-background
               (clj-http.client/get "signed url" anything) => {:body {:jws-signed-payload "test signed payload" :jku "jwk endpoint"}}
               (clj-http.client/get "unsigned url" anything) => {:body {}})

             (let [activity-sources {:signed-activity-src   {:name           "A. Another Source"
                                                             :url            "signed url"
                                                             :activity-types ["Type1"]}
                                     :unsigned-activity-src {:name           "B. Yet Another Source"
                                                             :url            "unsigned url"
                                                             :activity-types ["Type2"]}}
                   non-existent-user-feed-preferences nil
                   result (cf/generate-activity-source-preferences activity-sources non-existent-user-feed-preferences)]

               (fact "signed source should return true for signed activity sources"
                     (:id (first result)) => "signed-activity-src"
                     (:signed? (first result)) => true)

               ; FIXME find new way to check if activty src is signed without making http request (AW/JC 29/10)
               (future-fact "unsigned source should return false for signed?"
                     (:id (second result)) => "unsigned-activity-src"
                     (:signed? (second result)) => false))))
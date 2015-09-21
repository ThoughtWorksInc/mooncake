(ns mooncake.test.controller.customise-feed
  (:require [midje.sweet :refer :all]
            [ring.mock.request :as mock]
            [net.cgrand.enlive-html :as html]
            [mooncake.controller.customise-feed :as cf]
            [mooncake.db.user :as user]
            [mooncake.routes :as routes]
            [mooncake.test.test-helpers.enlive :as eh]
            [mooncake.test.test-helpers.db :as dbh]))

(facts "about customise-feed"
       (let [customise-feed-request {:context {:activity-sources {:activity-src         {:name           "A. Activity Source"
                                                                                         :url            "some url"
                                                                                         :activity-types '("Create" "Question")}
                                                                  :another-activity-src {:name           "B. Another Source"
                                                                                         :url            "another url"
                                                                                         :activity-types '("Question")}}}
                                     :params  {:activity-src            "foobar"
                                               "activity-src::Question" "anything"
                                               :some-other-param        "something-else"}
                                     :session {:username ...username...}}
             db (dbh/create-in-memory-db)
             stored-user (user/create-user! db ...user-id... ...username...)
             response (cf/customise-feed db customise-feed-request)]
         (fact "it should update the user's feed settings for all activity sources"

               (user/find-user db ...username...) => {:auth-provider-user-id ...user-id...
                                                      :username              ...username...
                                                      :feed-settings         {:activity-src         {:selected true
                                                                                                     :types    [{:id       "Create"
                                                                                                                 :selected false}
                                                                                                                {:id       "Question"
                                                                                                                 :selected true}]}
                                                                              :another-activity-src {:selected false
                                                                                                     :types    [{:id       "Question"
                                                                                                                 :selected false}]}}})
         (fact "it should disable activity types if parent activity source is disabled"
               (let [customise-feed-request {:context {:activity-sources {:activity-src         {:name           "A. Activity Source"
                                                                                                 :url            "some url"
                                                                                                 :activity-types '("Create" "Question")}}}
                                             :params  {"activity-src::Question" "anything"
                                                       "activity-src::Create"   "anything"
                                                       :some-other-param        "something-else"}
                                             :session {:username ...username...}}
                     db (dbh/create-in-memory-db)
                     _ (user/create-user! db ...user-id... ...username...)
                     _ (cf/customise-feed db customise-feed-request)]
               (user/find-user db ...username...) => {:auth-provider-user-id ...user-id...
                                                      :username              ...username...
                                                      :feed-settings         {:activity-src         {:selected false
                                                                                                     :types    [{:id       "Create"
                                                                                                                 :selected false}
                                                                                                                {:id       "Question"
                                                                                                                 :selected false}]}}}))
         (fact "it should redirect to /"
               response => (eh/check-redirects-to (routes/absolute-path {} :feed)))))

(facts "about show-customise-feed"
       (let [show-customise-feed-request {:context {:activity-sources {:activity-src             {:name           "A. Activity Source"
                                                                                                  :url            "some url"
                                                                                                  :activity-types '("Create" "Question")}
                                                                       :another-activity-src     {:name           "B. Another Source"
                                                                                                  :url            "another url"
                                                                                                  :activity-types '("Add")}
                                                                       :yet-another-activity-src {:name           "C. Yet Another Source"
                                                                                                  :url            "yet another url"
                                                                                                  :activity-types '("Add")}}
                                                    :translator       {}}
                                          :session {:username ...username...}}
             db (dbh/create-in-memory-db)
             _ (user/create-user! db ...user-id... ...username...)
             _ (user/update-feed-settings! db ...username... {:activity-src         {:selected true
                                                                                     :types    [{:id       "Create"
                                                                                                 :selected false}
                                                                                                {:id       "Question"
                                                                                                 :selected true}]}
                                                              :another-activity-src {:selected false
                                                                                     :types    [{:id       "Add"
                                                                                                 :selected false}]}})
             response (cf/show-customise-feed db show-customise-feed-request)]
         (fact "it should show available activity sources on feed settings page"
               response => (eh/check-renders-page [:.func--customise-feed-page])
               (:body response) => (contains "Activity Source")
               (:body response) => (contains "Another Source"))

         (fact "it should show available activity types for available sources on feed settings page"
               (:body response) => (contains "Create")
               (:body response) => (contains "Question")
               (:body response) => (contains "Add"))

         (fact "it should select activity sources based on user's feed settings"
               (let [[first-checkbox second-checkbox third-checkbox] (-> (html/html-snippet (:body response))
                                                                         (html/select [:.clj--feed-item__checkbox]))]

                 (-> first-checkbox :attrs :id) => "activity-src"
                 (-> first-checkbox :attrs :checked) => "checked"

                 (-> second-checkbox :attrs :id) => "another-activity-src"
                 (-> second-checkbox :attrs :checked) => nil

                 (-> third-checkbox :attrs :id) => "yet-another-activity-src"
                 (-> third-checkbox :attrs :checked) => "checked"))

         (fact "it should select activity types from activity sources based on user's feed settings"

               (let [[first-child-checkbox second-child-checkbox third-child-checkbox] (-> (html/html-snippet (:body response))
                                                                                           (html/select [:.clj--feed-item-child__checkbox]))]

                 (-> first-child-checkbox :attrs :id) => "activity-src::Create"
                 (-> first-child-checkbox :attrs :checked) => nil

                 (-> second-child-checkbox :attrs :id) => "activity-src::Question"
                 (-> second-child-checkbox :attrs :checked) => "checked"

                 (-> third-child-checkbox :attrs :id) => "another-activity-src::Add"
                 (-> third-child-checkbox :attrs :checked) => nil))

         (fact "it should select activity types which are not explicitly set in user preferences according to state of the parent feed source"

               (let [
                     _ (user/update-feed-settings! db ...username... {:activity-src         {:selected true
                                                                                             :types    [{:id       "Add"
                                                                                                         :selected false}
                                                                                                        ]}
                                                                      :another-activity-src {:selected false}})
                     response (cf/show-customise-feed db show-customise-feed-request)
                     [first-child-checkbox second-child-checkbox third-child-checkbox fourth-child-checkbox] (-> (html/html-snippet (:body response))
                                                                                                                 (html/select [:.clj--feed-item-child__checkbox]))]

                 (-> first-child-checkbox :attrs :id) => "activity-src::Create"
                 (-> first-child-checkbox :attrs :checked) => "checked"

                 (-> second-child-checkbox :attrs :id) => "activity-src::Question"
                 (-> second-child-checkbox :attrs :checked) => "checked"

                 (-> third-child-checkbox :attrs :id) => "another-activity-src::Add"
                 (-> third-child-checkbox :attrs :checked) => nil

                 (-> fourth-child-checkbox :attrs :id) => "yet-another-activity-src::Add"
                 (-> fourth-child-checkbox :attrs :checked) => "checked"))))


(facts "about generate-activity-source-preferences"
       (let [activity-sources {:activity-src             {:name "C. Activity Source"
                                                          :url  "some url"}
                               :another-activity-src     {:name "A. Another Source"
                                                          :url  "another url"}
                               :yet-another-activity-src {:name "B. Yet Another Source"
                                                          :url  "yet another url"}}
             user-feed-preferences {:activity-src         {:selected true}
                                    :another-activity-src {:selected false}}
             result (cf/generate-activity-source-preferences activity-sources user-feed-preferences)]

         (fact "they include activity source ids for all sources"
               (map :id result) => (contains ["activity-src" "another-activity-src" "yet-another-activity-src"] :in-any-order))
         (fact "they include activity source name for all sources"
               (map :name result) => (contains ["C. Activity Source" "A. Another Source" "B. Yet Another Source"] :in-any-order))
         (fact "they include activity source url for all sources"
               (map :url result) => (contains ["some url" "another url" "yet another url"] :in-any-order))
         (fact "they include 'selected' flag"
               (map :selected result) => [false true true])
         (fact "they are sorted by name"
               result => [{:name "A. Another Source" :id "another-activity-src" :url "another url" :selected false :activity-types []}
                          {:name "B. Yet Another Source" :id "yet-another-activity-src" :url "yet another url" :selected true :activity-types []}
                          {:name "C. Activity Source" :id "activity-src" :url "some url" :selected true :activity-types []}])))

(facts "about generate-activity-type-preferences"
       (let [avaiable-acitivty-type-from-source '("Create" "Add" "Question")
             user-feed-activity-type-settings [{:id       "Create"
                                                :selected false}
                                               {:id       "Question"
                                                :selected true}]]
         (fact "they include activity type IDs for all acivity types"
               (let [result (cf/generate-activity-type-preferences avaiable-acitivty-type-from-source user-feed-activity-type-settings true)]
                 (map :name result) => (contains ["Create" "Add" "Question"] :in-any-order)))
         (fact "they include 'selected' flag"
               (let [result (cf/generate-activity-type-preferences avaiable-acitivty-type-from-source user-feed-activity-type-settings true)]
                 (map :selected result) => [false true true]))
         (fact "they should not be selected if parent feed is not selected"
               (let [result (cf/generate-activity-type-preferences avaiable-acitivty-type-from-source user-feed-activity-type-settings false)]
                 (map :selected result) => [false false false]))))

(facts "about get-feed-preferences-for-activity-type"
       (let [user-feed-activity-type-settings [{:id       "Create"
                                                :selected false}
                                               {:id       "Question"
                                                :selected true}]]
         (fact "locates activity type settings for the given Id"
               (cf/get-feed-preferences-for-activity-type user-feed-activity-type-settings "Create") => {:id "Create" :selected false}
               (cf/get-feed-preferences-for-activity-type user-feed-activity-type-settings "Question") => {:id "Question" :selected true})
         (fact "craated default activity type settings entry if the requested entry is not available"
               (cf/get-feed-preferences-for-activity-type user-feed-activity-type-settings "Add") => {:id "Add" :selected true})))

(tabular
  (fact "about selected-feed? - true if not set"
        (cf/selected-feed? ?feed-setting-input) => ?feed-setting-output)
  ?feed-setting-input ?feed-setting-output
  true true
  false false
  nil true
  ...anything-else... true)

(facts "about create-user-feed-settings-for-source"
       (let [single-activity-source-configuration {:name           "A. Activity Source"
                                                   :url            "some url"
                                                   :activity-types '("Create" "Question")}]
         (fact "unselects all if no parameters submitted"
               (let [expected-user-feed-settings {:selected false
                                                  :types    [{:id       "Create"
                                                              :selected false}
                                                             {:id       "Question"
                                                              :selected false}]}]

                 (cf/create-user-feed-settings-for-source :activity-src single-activity-source-configuration {}) => expected-user-feed-settings))
         (fact "selects activity types which match submitted parameters"
               (let [submitted-parameters {:activity-src          "foo"
                                           "activity-src::Create" "bar"}
                     expected-user-feed-settings {:selected true
                                                  :types    [{:id       "Create"
                                                              :selected true}
                                                             {:id       "Question"
                                                              :selected false}]}]

                 (cf/create-user-feed-settings-for-source :activity-src single-activity-source-configuration submitted-parameters) => expected-user-feed-settings))
         (fact "deselects activity types if parent activity source is deselected"
               (let [submitted-parameters {"activity-src::Create" "bar"}
                     expected-user-feed-settings {:selected false
                                                  :types    [{:id       "Create"
                                                              :selected false}
                                                             {:id       "Question"
                                                              :selected false}]}]

                 (cf/create-user-feed-settings-for-source :activity-src single-activity-source-configuration submitted-parameters) => expected-user-feed-settings))))


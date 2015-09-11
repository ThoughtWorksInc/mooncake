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
       (let [customise-feed-request {:context {:activity-sources {:activity-src         {:name "A. Activity Source"
                                                                                         :url  "some url"}
                                                                  :another-activity-src {:name "B. Another Source"
                                                                                         :url  "another url"}}}
                                     :params  {:activity-src     "foobar"
                                               :some-other-param "something-else"}
                                     :session {:username ...username...}}
             db (dbh/create-in-memory-db)
             stored-user (user/create-user! db ...user-id... ...username...)
             response (cf/customise-feed db customise-feed-request)]
         (fact "it should update the user's feed settings for all activity sources"
               (user/find-user db ...username...) => {:auth-provider-user-id ...user-id...
                                                      :username              ...username...
                                                      :feed-settings         {:activity-src         true
                                                                              :another-activity-src false}})

         (fact "it should redirect to /"
               response => (eh/check-redirects-to (routes/absolute-path {} :feed)))))

(facts "about show-customise-feed"
       (let [show-customise-feed-request {:context {:activity-sources {:activity-src             {:name "A. Activity Source"
                                                                                                  :url  "some url"}
                                                                       :another-activity-src     {:name "B. Another Source"
                                                                                                  :url  "another url"}
                                                                       :yet-another-activity-src {:name "C. Yet Another Source"
                                                                                                  :url  "yet another url"}}
                                                    :translator {}}
                                          :session {:username ...username...}}
             db (dbh/create-in-memory-db)
             _ (user/create-user! db ...user-id... ...username...)
             _ (user/update-feed-settings! db ...username... {:activity-src true :another-activity-src false})
             response (cf/show-customise-feed db show-customise-feed-request)]
         (fact "it should show available activity sources on feed settings page"
               response => (eh/check-renders-page [:.func--customise-feed-page])
               (:body response) => (contains "Activity Source")
               (:body response) => (contains "Another Source"))

         (fact "it should select activity sources based on user's feed settings"
               (let [[first-checkbox second-checkbox third-checkbox] (-> (html/html-snippet (:body response))
                                                                         (html/select [:.clj--feed-item__checkbox]))]

                 (-> first-checkbox :attrs :id) => "activity-src"
                 (-> first-checkbox :attrs :checked) => "checked"

                 (-> second-checkbox :attrs :id) => "another-activity-src"
                 (-> second-checkbox :attrs :checked) => nil

                 (-> third-checkbox :attrs :id) => "yet-another-activity-src"
                 (-> third-checkbox :attrs :checked) => "checked"))))


(facts "about generate-activity-source-preferences"
       (let [activity-sources {:activity-src             {:name "C. Activity Source"
                                                          :url  "some url"}
                               :another-activity-src     {:name "A. Another Source"
                                                          :url  "another url"}
                               :yet-another-activity-src {:name "B. Yet Another Source"
                                                          :url  "yet another url"}}
             user-feed-preferences {:activity-src         true
                                    :another-activity-src false}
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
               result => [{:name "A. Another Source" :id "another-activity-src" :url "another url" :selected false}
                          {:name "B. Yet Another Source" :id "yet-another-activity-src" :url "yet another url" :selected true}
                          {:name "C. Activity Source" :id "activity-src" :url "some url" :selected true}])))

(tabular
  (fact "about selected-feed? - true if not set"
        (cf/selected-feed? ?feed-setting-input) => ?feed-setting-output)
  ?feed-setting-input      ?feed-setting-output
  true                     true
  false                    false
  nil                      true
  ...anything-else...      true)


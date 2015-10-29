(ns mooncake.test.controller.feed
  (:require [midje.sweet :refer :all]
            [mooncake.controller.feed :as fc]
            [mooncake.activity :as a]
            [mooncake.test.test-helpers.enlive :as eh]
            [mooncake.test.test-helpers.db :as dbh]
            [mooncake.db.user :as user]
            [mooncake.db.mongo :as mongo]
            [mooncake.db.activity :as adb]
            [mooncake.config :as config]
            [mooncake.test.test-helpers.activities :as act]))

(def ten-oclock "2015-01-01T10:00:00.000Z")
(def eleven-oclock "2015-01-01T11:00:00.000Z")
(def twelve-oclock "2015-01-01T12:00:00.000Z")
(def next-day "2015-01-02T12:00:00.000Z")
(def previous-day "2014-12-31T12:00:00.000Z")

(fact "feed handler displays activities retrieved from activity sources"
      (let [store (dbh/create-in-memory-store)]
        (mongo/store! store adb/activity-collection {:actor            {(keyword "@type") "Person"
                                                                      :displayName      "JDog"}
                                                   :published        ten-oclock
                                                   :activity-src     "OpenAhjo"
                                                   (keyword "@type") "Activity"})
        (mongo/store! store adb/activity-collection {:actor            {(keyword "@type") "Person"
                                                                      :displayName      "KCat"}
                                                   :published        twelve-oclock
                                                   :activity-src     "objective8"
                                                   (keyword "@type") "Activity"})
        (fc/feed store {:context
                           {:activity-sources {:OpenAhjo   {:activity-types ["Activity"]}
                                               :objective8 {:activity-types ["Activity"]}}}
                        :t (constantly "")}) => (every-checker
                                                  (eh/check-renders-page :.func--feed-page)
                                                  (contains {:body (contains "JDog")})
                                                  (contains {:body (contains "KCat")}))))

(fact "feed handler displays username of logged-in user"
      (fc/feed (dbh/create-in-memory-store) {:t       (constantly "")
                                             :session {:username "Barry"}}) => (contains {:body (contains "Barry")}))

(def activity-src-1--enabled-type {:actor            {(keyword "@type") "Person"
                                                      :displayName      "Activity source 1: enabled type"}
                                   :published        ten-oclock
                                   :activity-src     "activity-src-1"
                                   (keyword "@type") "Enabled"})
(def activity-src-1--disabled-type {:actor            {(keyword "@type") "Person"
                                                       :displayName      "Activity source 1: disabled type"}
                                    :published        eleven-oclock
                                    :activity-src     "activity-src-1"
                                    (keyword "@type") "Disabled"})
(def activity-src-2--no-preference-type {:actor            {(keyword "@type") "Person"
                                                            :displayName      "Activity source 2: no preference expressed"}
                                         :published        twelve-oclock
                                         :activity-src     "activity-src-2"
                                         (keyword "@type") "No-preference"})

(facts "about which activities feed handler displays"
       (let [store (dbh/create-in-memory-store)
             _ (mongo/store! store adb/activity-collection activity-src-1--enabled-type)
             _ (mongo/store! store adb/activity-collection activity-src-1--disabled-type)
             _ (mongo/store! store adb/activity-collection activity-src-2--no-preference-type)
             _ (user/create-user! store ...user-id... ...username...)
             _ (user/update-feed-settings! store ...username... {:activity-src-1 {:types [{:id "Enabled" :selected true}
                                                                                          {:id "Disabled" :selected false}]}})
             request {:context {:activity-sources {:activity-src-1 {:activity-types ["Enabled" "Disabled"]}
                                                   :activity-src-2 {:activity-types ["No-preference"]}}}
                      :t       (constantly "")
                      :session {:username ...username...}}
             response (fc/feed store request)]


         (fact "enabled activity types are shown"
               (:body response) => (contains "Activity source 1: enabled type"))

         (fact "disabled activity types are not shown"
               (:body response) =not=> (contains "Activity source 1: disabled type"))

         (fact "no-preference activity types are shown"
               (:body response) => (contains "Activity source 2: no preference expressed"))

         (fact "custom message is not shown when there are activities on the page"
               (:body response) =not=> (contains "clj--empty-activity-item"))

         (fact "custom message is shown if all activity types are disabled"
               (user/update-feed-settings! store ...username... {:activity-src-1 {:types [{:id "Disabled" :selected false}]}})
               (let [no-activities-request {:context {:activity-sources {:activity-src-1 {:activity-types ["Disabled"]}}}
                                            :t       (constantly "")
                                            :session {:username ...username...}}
                     no-activities-response (fc/feed store no-activities-request)]
                 (:body no-activities-response) => (contains "clj--empty-activity-item")))))

(facts "about generating the activity query map"
       (let [feed-settings {:activity-src-1 {:types [{:id "Enabled" :selected true}
                                                     {:id "Disabled" :selected false}]}
                            :activity-src-2 {:types [{:id "Disabled" :selected false}]}}

             activity-sources {:activity-src-1 {:activity-types ["Enabled" "Disabled" "No-preference"]}
                               :activity-src-2 {:activity-types ["Disabled"]}
                               :activity-src-3 {:activity-types ["No-preference"]}}]

         (fc/generate-feed-query feed-settings activity-sources) => (just [{:activity-src     "activity-src-1"
                                                                            (keyword "@type") ["Enabled" "No-preference"]}
                                                                           {:activity-src     "activity-src-3"
                                                                            (keyword "@type") ["No-preference"]}] :in-any-order)))

(facts "about pagination"
       (let [store (dbh/create-in-memory-store)
             _ (user/create-user! store ...user-id... ...username...)
             _ (dbh/create-dummy-activities store (+ 1 config/activities-per-page))
             valid-page-number "2"
             invalid-page-number "ABC"
             too-tiny-of-a-page-number "0"
             too-big-of-a-page-number "3"]

             (fact "page number is passed in get request"
                   (let [request {:context {:activity-sources {:test-source {:activity-types ["Create"]}}}
                                  :t       (constantly "")
                                  :session {:username ...username...}
                                  :params  {:page-number valid-page-number}}
                         response (fc/feed store request)]

                     (:body response) => (contains "TestData0")
                     (:body response) =not=> (contains (str "TestData" config/activities-per-page))))

             (fact "empty page number params is passed in get request and defaults to 1"
                   (let [request {:context {:activity-sources {:test-source {:activity-types ["Create"]}}}
                                  :t       (constantly "")
                                  :session {:username ...username...}
                                  :params  {}}
                         response (fc/feed store request)]

                     (:body response) =not=> (contains "TestData0")
                     (:body response) => (contains (str "TestData" config/activities-per-page))))

             (fact "page number cannot be non-numbers"
                   (let [request {:context {:activity-sources {:test-source {:activity-types ["Create"]}}}
                                  :t       (constantly "")
                                  :session {:username ...username...}
                                  :params  {:page-number invalid-page-number}}
                         response (fc/feed store request)]

                     (:status response) => nil))

             (fact "page number cannot be too small"
                   (let [request {:context {:activity-sources {:test-source {:activity-types ["Create"]}}}
                                  :t       (constantly "")
                                  :session {:username ...username...}
                                  :params  {:page-number too-tiny-of-a-page-number}}
                         response (fc/feed store request)]

                     (:status response) => nil))

             (fact "page number cannot be too big"
                   (let [request {:context {:activity-sources {:test-source {:activity-types ["Create"]}}}
                                  :t       (constantly "")
                                  :session {:username ...username...}
                                  :params  {:page-number too-big-of-a-page-number}}
                         response (fc/feed store request)]

                     (:status response) => nil))))

(defn remove-whitespace-and-new-lines [s]
  (-> s
      (clojure.string/replace " " "")
      (clojure.string/replace "\n" "")))

(fact "activities are transformed into the correct json format"
      (let [activities [act/activity-KCat
                        act/activity-JDog]
            request {}
            json (a/activities->json activities request)]

        (remove-whitespace-and-new-lines json) => (remove-whitespace-and-new-lines (str "{\"activities\":[" act/json-for-KCat "," act/json-for-JDog "]}"))))

(defn request-with-timestamp [timestamp-params]
  {:context {:activity-sources {:activity-src-1 {:activity-types ["Enabled" "Disabled"]}
                                :activity-src-2 {:activity-types ["No-preference"]}}}
   :t       (constantly "")
   :session {:username ...username...}
   :params timestamp-params})

(facts "about which activities are retrieved and updated"
       (let [store (dbh/create-in-memory-store)
             _ (mongo/store! store adb/activity-collection activity-src-1--enabled-type)
             _ (mongo/store! store adb/activity-collection activity-src-1--disabled-type)
             _ (mongo/store! store adb/activity-collection activity-src-2--no-preference-type)
             _ (user/create-user! store ...user-id... ...username...)
             _ (user/update-feed-settings! store ...username... {:activity-src-1 {:types [{:id "Enabled" :selected true}
                                                                                          {:id "Disabled" :selected false}]}})
             response-for-retrieving (fc/retrieve-activities store (request-with-timestamp {:timestamp next-day}))
             response-for-updating (fc/retrieve-activities store (request-with-timestamp {:timestamp-from previous-day}))]
         (facts "retrieving older activities"

                (fact "enabled activity types are shown"
                      (:body response-for-retrieving) => (contains "Activity source 1: enabled type"))

                (fact "disabled activity types are not shown"
                      (:body response-for-retrieving) =not=> (contains "Activity source 1: disabled type"))

                (fact "no-preference activity types are shown"
                      (:body response-for-retrieving) => (contains "Activity source 2: no preference expressed")))

         (facts "retrieving newer activities"

                (fact "enabled activity types are shown"
                      (:body response-for-updating) => (contains "Activity source 1: enabled type"))

                (fact "disabled activity types are not shown"
                      (:body response-for-updating) =not=> (contains "Activity source 1: disabled type"))

                (fact "no-preference activity types are shown"
                      (:body response-for-updating) => (contains "Activity source 2: no preference expressed")))))


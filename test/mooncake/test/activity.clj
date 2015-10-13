(ns mooncake.test.activity
  (:require [midje.sweet :refer :all]
            [clj-http.client :as http]
            [mooncake.activity :as a]
            [mooncake.test.test-helpers.db :as dbh]
            [mooncake.db.activity :as activity])
  (:import (java.net ConnectException)))

(def nine-oclock "2015-01-01T09:00:00.000Z")
(def ten-oclock "2015-01-01T10:00:00.000Z")
(def eleven-oclock "2015-01-01T11:00:00.000Z")
(def twelve-oclock "2015-01-01T12:00:00.000Z")

(fact "about retrieving unsigned activity responses"
      (fact "poll-activity-sources retrieves activities from multiple sources, sorts them by published time, assocs activity source and signed status into each activity "
            (let [an-activity-src-url "https://an-activity.src"
                  another-activity-src-url "https://another-activity.src"
                  store (dbh/create-in-memory-store)]
              (a/poll-activity-sources store {:an-activity-src      {:url an-activity-src-url}
                                              :another-activity-src {:url another-activity-src-url}})
              => [{:activity-src     :an-activity-src
                   :actor            {:displayName "KCat"}
                   :published        twelve-oclock
                   (keyword "@type") "Add"
                   :signed           false}
                  {:activity-src     :another-activity-src
                   :actor            {:displayName "LSheep"}
                   :published        eleven-oclock
                   (keyword "@type") "Create"
                   :signed           false}
                  {:activity-src     :an-activity-src
                   :actor            {:displayName "JDog"}
                   :published        ten-oclock
                   (keyword "@type") "Add"
                   :signed           false}]
              (provided
                (http/get an-activity-src-url {:accept :json
                                               :as     :json}) => {:body [{:actor            {:displayName "JDog"}
                                                                           :published        ten-oclock
                                                                           (keyword "@type") "Add"}
                                                                          {:actor            {:displayName "KCat"}
                                                                           :published        twelve-oclock
                                                                           (keyword "@type") "Add"}]}
                (http/get another-activity-src-url {:accept :json
                                                    :as     :json}) => {:body [{:actor            {:displayName "LSheep"}
                                                                                :published        eleven-oclock
                                                                                (keyword "@type") "Create"}]})))

      (fact "poll-activity-sources does not retrieve activities that have invalid format"
            (let [an-activity-src-url "https://an-activity.src"
                  store (dbh/create-in-memory-store)]
              (a/poll-activity-sources store {:an-activity-src {:url an-activity-src-url}})
              => [{:activity-src     :an-activity-src
                   :actor            {:displayName "JDog"}
                   :published        ten-oclock
                   (keyword "@type") "Add"
                   :signed           false}]
              (provided
                (http/get an-activity-src-url {:accept :json
                                               :as     :json}) => {:body [{:actor            {:displayName "JDog"}
                                                                           :published        ten-oclock
                                                                           (keyword "@type") "Add"}
                                                                          {:actor {:displayName "KCat"}}]}))))

(facts "about retrieving signed activity responses"
       (fact "activities can be retrieved from a source that signs its responses using json web signatures and json web keys"
             (a/poll-activity-sources (dbh/create-in-memory-store) {:signed-activity-source {:url "signed-activity-source-url"}})
             => [{(keyword "@type") "Create"
                  :activity-src     :signed-activity-source
                  :published        "2015-10-06T11:23:50.000Z"
                  :signed           true}
                 {(keyword "@type") "Add"
                  :activity-src     :signed-activity-source
                  :published        "2015-10-06T11:23:45.000Z"
                  :signed           true}]
             (provided
               (http/get "signed-activity-source-url" {:accept :json
                                                       :as     :json}) => {:body {:jku                "json-web-key-set-url"
                                                                                  :jws-signed-payload "eyJhbGciOiJSUzI1NiJ9.W3siQHR5cGUiOiJDcmVhdGUiLCJwdWJsaXNoZWQiOiIyMDE1LTEwLTA2VDExOjIzOjUwLjAwMFoifSx7IkB0eXBlIjoiQWRkIiwicHVibGlzaGVkIjoiMjAxNS0xMC0wNlQxMToyMzo0NS4wMDBaIn1d.QofcptlnRdIJZo8tSWyl9GiBGDxvb0D1LLbjCHqU9NsBnO49YjUnAaRaXA0Kc6higDZI3wsG4GjBPrOwkeblNookxNDTgY4nlNUMNIhKyFIop8ATq-dzeug5yKvusB3bqJcF0VoVL4myn9ZPJF5iIsFmV-GM_NYpImUlJLemCW1UWyMFw_beg061fWz_CeTJRTO05ZO-xwjSgjz_Ip7E7RUjsoyxUztlrGUzBfFu6L9uSXeBy_3IJ-qZF4N9rYvjgXUg304M-cxjZ3g-EQgSgtlaxWXhmIf8xapGSHALd_YUiEedSN6GbUFDoaeHOWj3NkYWAwmTtf86DysQa8gYGA"}}
               (http/get "json-web-key-set-url" {:accept :json}) => {:body "{\"keys\": [{\"kty\": \"RSA\", \"kid\": \"key-1444312448597\", \"alg\": \"RS256\", \"n\":   \"xoGFEME7awEBqRVzbSl-q1PA67KIRus_E9t25WAJgfZ9ynZVMlFwcozJSMf2mFaSV3DHR_X6o9kzaTCfklFuISshlYXvi9torY6CYn_InALOCRVTaV_bElSjvCVrlEw23hveAfOWT9JfCtPniSVCbt75UPZ8ewkC0sNNZsc4a4XbMKVirk6-g6XPUYhQAPfCc2pUzJYZDFLkgl39kk2s_UkFwLgFljNIawr4nz2vnAwfFYpJP67qGM1DCZmtlJCR90MlzMSQiSaCy9TFcfUKnWDJ_hFeaP9a1HfqKY_M0R0CNNsQZLbV2DttXq_jf77QtrDV8URd9iWuIg8ncflX9Q\", \"e\":   \"AQAB\"}]}"}))


       (fact "if verification fails (e.g. due to an invalid jku (json-web-key-set-url)) then activities should be retrieved as unsigned (no :signed key)"
             (a/poll-activity-sources (dbh/create-in-memory-store) {:signed-activity-source {:url "signed-activity-source-url"}})
             => [{(keyword "@type") "Create"
                  :activity-src     :signed-activity-source
                  :published        "2015-10-06T11:23:50.000Z"}
                 {(keyword "@type") "Add"
                  :activity-src     :signed-activity-source
                  :published        "2015-10-06T11:23:45.000Z"}]
             (provided
               (http/get "signed-activity-source-url" {:accept :json
                                                       :as     :json}) => {:body {:jku                "OOPS_WRONG"
                                                                                  :jws-signed-payload "eyJhbGciOiJSUzI1NiJ9.W3siQHR5cGUiOiJDcmVhdGUiLCJwdWJsaXNoZWQiOiIyMDE1LTEwLTA2VDExOjIzOjUwLjAwMFoifSx7IkB0eXBlIjoiQWRkIiwicHVibGlzaGVkIjoiMjAxNS0xMC0wNlQxMToyMzo0NS4wMDBaIn1d.QofcptlnRdIJZo8tSWyl9GiBGDxvb0D1LLbjCHqU9NsBnO49YjUnAaRaXA0Kc6higDZI3wsG4GjBPrOwkeblNookxNDTgY4nlNUMNIhKyFIop8ATq-dzeug5yKvusB3bqJcF0VoVL4myn9ZPJF5iIsFmV-GM_NYpImUlJLemCW1UWyMFw_beg061fWz_CeTJRTO05ZO-xwjSgjz_Ip7E7RUjsoyxUztlrGUzBfFu6L9uSXeBy_3IJ-qZF4N9rYvjgXUg304M-cxjZ3g-EQgSgtlaxWXhmIf8xapGSHALd_YUiEedSN6GbUFDoaeHOWj3NkYWAwmTtf86DysQa8gYGA"}}
               (http/get "OOPS_WRONG" anything) =throws=> (ConnectException.)))

       (fact "if the signed payload cannot be decoded then no activites are returned"
             (a/poll-activity-sources (dbh/create-in-memory-store) {:signed-activity-source {:url "signed-activity-source-url"}}) => []
             (provided
               (http/get "signed-activity-source-url" {:accept :json
                                                       :as     :json}) => {:body {:jku                "OOPS_WRONG"
                                                                                  :jws-signed-payload "NOT_A_VALID_ENCODED_PAYLOAD"}}))

       (fact "if the json message has invalid format then exception is thrown"
             (a/verify-and-return-activities ...signed-activity-source-url... ...jws...) => (throws Exception)
             (provided
               (a/verify-and-return-payload ...signed-activity-source-url... ...jws...) => "not-a-json")))

(facts "about validating format of the activities"
       (fact "activities that do not contain required attributes are filtered and logged"
             (let [invalid-activity {(keyword "@context") "http://www.w3.org/ns/activitystreams"
                                     (keyword "@type")    "Create"
                                     :actor               {(keyword "@type") "Person"
                                                           :displayName      "Barry"}}
                   valid-activity {(keyword "@context") "http://www.w3.org/ns/activitystreams"
                                   (keyword "@type")    "Add"
                                   :published           "2015-08-03T14:49:38.407Z"
                                   :actor               {(keyword "@type") "Person"
                                                         :displayName      "Joshua"}}
                   activities [invalid-activity valid-activity]]
               (a/remove-invalid-activities activities) => [valid-activity]
               (provided
                 (a/log-invalid-activity invalid-activity {:published :blank}) => anything))))

(fact "can load activity sources from a resource"
      (a/load-activity-sources "test-activity-sources.yml") => {:test-activity-source-1 {:url  "https://test-activity.src/activities"
                                                                                         :name "Test Activity Source 1"}
                                                                :test-activity-source-2 {:url  "https://another-test-activity.src"
                                                                                         :name "Test Activity Source 2"}
                                                                :test-activity-source-3 {:url  "https://yet-another-test-activity.src"
                                                                                         :name "Test Activity Source 3"}})

(fact "get-json-from-activity-source gracefully handles exceptions caused by bad/missing responses"
      (a/get-json-from-activity-source ...invalid-activity-src-url... nil) => nil
      (provided
        (http/get ...invalid-activity-src-url...
                  {:accept :json :as :json}) =throws=> (ConnectException.)))

(facts "sync activities retrieves activities from api and stores them"
       (let [an-activity-src-url "https://an-activity.src"
             another-activity-src-url "https://another-activity.src"
             json-src1 [{:actor            {:displayName "JDog"}
                         :published        ten-oclock
                         (keyword "@type") "a-type"}
                        {:actor            {:displayName "KCat"}
                         :published        twelve-oclock
                         (keyword "@type") "another-type"}]
             json-src2 [{:actor            {:displayName "LSheep"}
                         :published        eleven-oclock
                         (keyword "@type") "yet-another-type"}]
             store (dbh/create-in-memory-store)]
         (facts "with stubbed activity retrieval"
                (against-background
                  (http/get an-activity-src-url anything) => {:body json-src1}
                  (http/get another-activity-src-url anything) => {:body json-src2})
                (fact "activities are stored"
                      (a/sync-activities! store {:an-activity-src      {:url an-activity-src-url}
                                                 :another-activity-src {:url another-activity-src-url}})
                      (count (activity/fetch-activities store)) => 3)
                (fact "activities are not stored again"
                      (count (activity/fetch-activities store)) => 3
                      (a/sync-activities! store {:an-activity-src      {:url an-activity-src-url}
                                                 :another-activity-src {:url another-activity-src-url}})
                      (count (activity/fetch-activities store)) => 3)
                (fact "activity types are stored"
                      (a/retrieve-activity-types store) => {:an-activity-src      ["a-type" "another-type"]
                                                            :another-activity-src ["yet-another-type"]}))))

(facts "retrieve-activities-from-source"
       (let [activity-url "https://an-activity.src"
             activity-source :test-activity-source]

         (fact "make get request with no extra params if no recent-published-time is found"
               (let [store (dbh/create-in-memory-store)]
                 (a/retrieve-activities-from-source store [activity-source {:url activity-url}])) => anything
               (provided
                 (http/get activity-url {:accept :json
                                         :as     :json}) => {}))

         (fact "make get request with most recent published time stored"
               (let [store (dbh/create-in-memory-store)
                     recent-activity {:activity-src activity-source
                                      :published    ten-oclock}
                     old-activity {:activity-src activity-source
                                   :published    nine-oclock}]
                 (activity/store-activity! store recent-activity)
                 (activity/store-activity! store old-activity)
                 (a/retrieve-activities-from-source store [activity-source {:url activity-url}])) => anything

               (provided
                 (http/get activity-url {:accept       :json
                                         :as           :json
                                         :query-params {:from ten-oclock}}) => {}))))

(fact "can retrieve total number of activities queried"
      (let [store (dbh/create-in-memory-store)
            _ (dbh/create-dummy-activities store 60)
            feed-query [{:activity-src     "test-source"
                         (keyword "@type") ["Create"]}]]
        (a/total-count-by-feed store feed-query) => 60))

(tabular
  (fact "is-last-page returns true when on the last page"
        (a/is-last-page? ?page-number ?total-activities) => ?result)
        ?page-number   ?total-activities   ?result
         1              30                  true
         2              60                  true
         2              101                 false
         3              150                 true)

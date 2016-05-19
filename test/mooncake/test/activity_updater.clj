(ns mooncake.test.activity-updater
  (:require [mooncake.test.test-helpers.db :as dbh]
            [mooncake.activity-updater :as au]
            [clj-http.client :as http]
            [midje.sweet :refer :all]
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
              (au/poll-activity-sources store {:an-activity-src      {:url an-activity-src-url}
                                               :another-activity-src {:url another-activity-src-url}})
              => [{:activity-src :an-activity-src
                   :actor        {:name "KCat"}
                   :published    twelve-oclock
                   :type         "Add"
                   :signed       false}
                  {:activity-src :another-activity-src
                   :actor        {:name "LSheep"}
                   :published    eleven-oclock
                   :type         "Create"
                   :signed       false}
                  {:activity-src :an-activity-src
                   :actor        {:name "JDog"}
                   :published    ten-oclock
                   :type         "Add"
                   :signed       false}]
              (provided
                (http/get an-activity-src-url {:accept :json
                                               :as     :json}) => {:body {(keyword "@context") "http://www.w3.org/ns/activitystreams"
                                                                          :type                "Collection"
                                                                          :name                "Activity stream"
                                                                          :totalItems          2
                                                                          :items               [{:actor     {:name "JDog"}
                                                                                                 :published ten-oclock
                                                                                                 :type      "Add"}
                                                                                                {:actor     {:name "KCat"}
                                                                                                 :published twelve-oclock
                                                                                                 :type      "Add"}]}}
                (http/get another-activity-src-url {:accept :json
                                                    :as     :json}) => {:body {(keyword "@context") "http://www.w3.org/ns/activitystreams"
                                                                               :type                "Collection"
                                                                               :name                "Activity stream"
                                                                               :totalItems          1
                                                                               :items               [{:actor     {:name "LSheep"}
                                                                                                      :published eleven-oclock
                                                                                                      :type      "Create"}]}})))

      (fact "poll-activity-sources does not retrieve activities that have invalid format"
            (let [an-activity-src-url "https://an-activity.src"
                  store (dbh/create-in-memory-store)]
              (au/poll-activity-sources store {:an-activity-src {:url an-activity-src-url}})
              => [{:activity-src :an-activity-src
                   :actor        {:name "JDog"}
                   :published    ten-oclock
                   :type         "Add"
                   :signed       false}]
              (provided
                (http/get an-activity-src-url {:accept :json
                                               :as     :json}) => {:body {(keyword "@context") "http://www.w3.org/ns/activitystreams"
                                                                          :type                "Collection"
                                                                          :name                "Activity stream"
                                                                          :totalItems          3
                                                                          :items               [{:actor     {:name "JDog"}
                                                                                                 :published ten-oclock
                                                                                                 :type      "Add"}
                                                                                                {:actor            {:name "LSheep"}
                                                                                                 :published        ten-oclock
                                                                                                 (keyword "@type") "Create"}
                                                                                                {:actor {:name "KCat"}}]}}))))

(facts "about retrieving signed activity responses"
       (fact "activities can be retrieved from a source that signs its responses using json web signatures and json web keys"
             (au/poll-activity-sources (dbh/create-in-memory-store) {:signed-activity-source {:url "signed-activity-source-url"}})
             => [{:type         "Create"
                  :activity-src :signed-activity-source
                  :published    "2015-10-06T11:23:50.000Z"
                  :signed       true}
                 {:type         "Add"
                  :activity-src :signed-activity-source
                  :published    "2015-10-06T11:23:45.000Z"
                  :signed       true}]
             (provided
               (http/get "signed-activity-source-url" {:accept :json
                                                       :as     :json}) => {:body {:jku                "json-web-key-set-url"
                                                                                  :jws-signed-payload "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJAY29udGV4dCI6Imh0dHA6Ly93d3cudzMub3JnL25zL2FjdGl2aXR5c3RyZWFtcyIsInR5cGUiOiJDb2xsZWN0aW9uIiwibmFtZSI6IkFjdGl2aXR5IHN0cmVhbSIsInRvdGFsSXRlbXMiOjIsIml0ZW1zIjpbeyJ0eXBlIjoiQ3JlYXRlIiwicHVibGlzaGVkIjoiMjAxNS0xMC0wNlQxMToyMzo1MC4wMDBaIn0seyJ0eXBlIjoiQWRkIiwicHVibGlzaGVkIjoiMjAxNS0xMC0wNlQxMToyMzo0NS4wMDBaIn1dfQ.SsZ8kvPAbZmUO1PGQTkrElPDyoy8JXtSho0w-tFE0PGZzEVnVnRTRA-it86GUtf7kgFKLfGCtEK7sB5k6kS2GvHYqI-ZzioE9KU6uSd_6cjbwM7WYyYYjU7okDmM2eoc27QUV9eNDxDH5SyiiTug2YQ_3Iro64Ycn1WhRBaYBPvrotJ7wcWZDBq9GPncsn1sWrnVfKYyUgpe8YYWc6bCib1-fBtzuPZCh04rWC_H1cIM8rDRrtgaOdS8_G0v8uuBAqPezNNt2_Pu7MZLlcLeOz_45NzCUwh3_PpWfYC5--MoC3LH32hKFOhrbbuCONG48JwqrWPOEuM7BQrrtkd0ow"}}
               (http/get "json-web-key-set-url" {:accept :json}) => {:body "{\"keys\": [{\"kty\": \"RSA\", \"use\": \"sig\", \"n\": \"AMIhK0LBThtw3lGPC9rgyWdD96shgvYmoQRwPl7fiIRXqzzd8sqqIXU4I_mCI7DkccdYVJrbKma-wAoCt6ThfR1s9U5BP6SnZK6iFk3SfcAG4JV4zndBKZtx3dqZqQjzGarL5PbEKsB0xy5tVYb-nvrF-vIaWeY62PSx7cjMZ0YcOmel99omgnwBp6nSyN-gdK7QIooYtsRo3eL0eTf6zJnSa8wu61d1QsH-6yQiMYeV2JaBQj8QoE2KfHq-OwGCWvN5_CvIDFN4Qy5mAWKimU3sfRw4OkQv1kGZ-q52Z3hCJg1KKcSEY5MDZpkLckHqNBNjDcx4NCtuMwjfcrQ_Igk\", \"e\": \"AQAB\", \"d\": \"cshl6dyeMD92VEb-PXa33yUi5b60zpJclmE_n50P_SBREXYyPn6Ftedx7e9y5v7L_5BMxhtcYM_cgI7GwujIr4NjL9gIp3SKZW9VPMJ-s_HdDMQXYA_ZaB3VjZFZjv8eaAyS3w1yMcVamCrxbRZULfatwESwbx7QdS5XaGjjj2HNHWnQvzb0HMY0aMFo_H4AqcIBdpy_m3FoWsQbG7P6wcQpy4y1zkZbnHcDVf8c08NPXbG935mYMUdx9HDirMOiSak-blmXBWk8uNQJgvwKczHS9LyV6r-ON7KfzhXzCKpGbOft2P78WthcahEQLd8H6wMUntMssMER2CC32HmvWQ\", \"p\": \"AO-uB4f1kim19N5i3yAV_jqebVyvMs5d0LqsESgm1TvOle_mfxpjHQE7k3URcs6N-_y3tdMYlwwRryZmKYxkAQWqcOcI_IE463WstyZfaUrpf0xLJwGIsbZzzXydhC6FOc5dJ1gMTz5l6ONbBCSO7A8kAeeIZJHvhIGGzyJzGxO_\", \"q\": \"AM9ZIdM74Q1XzKSa4aAbfotZ82tvYh7xPmj6NhVHpI1BJ8TfvNXPcKXRnDs6sNkKK0ksi5egMhk-Cg_oTH4c7awIXIV86C4NRPM6nngiOWpooFz_NbK345fLks7l5nE-1u6qALz4HOdhbAPGS_FjPrDR3lS5HjGsfQbKGMUZ-xw3\", \"dp\": \"WhCobdO-6AOjD4pR1CnPjdGIwQJo8hlY3TzZeaAWEtJPj4WrD4xdEuCDScOTw8ChB1c1cSzVXcira5-KT2Io7CsfIAJFeH2eJWsQq8_ArlDN8Cpxbuch-LDNb911FVIk5cIljbWadZUwDXdfOCmo1Quv14RuXlSGE3JIFebxLts\", \"dq\": \"AIpeyfLkN9imqfuDHGSzVGx8V7RvfUR265Y0u9jRmZ9mRrrcMHFi4KLX0fG4xgHhBmfroTBLiINN4nshI8LZXUZ7wfqXE35__m5uxQgYlsZLEhFdgqFElE5dXRhTVchnDhnxO1LgJLHIUsPmFhH9j_2B4GQbsWmm2Typq4QFRY1b\", \"qi\": \"AK8_OjKYFcJY358f7FSxvwlPNO22JgycHsNgBQK3G43TFLh44mgEg3_pZlMS4Qlam4aV_zs8hy6rTIccbPhRW9i3f4bI8i9jWp4DZz4XDQBwkTdJDVsABsJJSLd6YUWGsl32hEU-JLX-07KyL3zby0eQl7qn5OPmVloVampCYD7D\" }]}"}))


       (fact "if verification fails (e.g. due to an invalid jku (json-web-key-set-url)) then activities should be retrieved as verification-failed"
             (au/poll-activity-sources (dbh/create-in-memory-store) {:signed-activity-source {:url "signed-activity-source-url"}})
             => [{:type         "Create"
                  :activity-src :signed-activity-source
                  :published    "2015-10-06T11:23:50.000Z"
                  :signed       :verification-failed}
                 {:type         "Add"
                  :activity-src :signed-activity-source
                  :published    "2015-10-06T11:23:45.000Z"
                  :signed       :verification-failed}]
             (provided
               (http/get "signed-activity-source-url" {:accept :json
                                                       :as     :json}) => {:body {:jku                "OOPS_WRONG"
                                                                                  :jws-signed-payload "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJAY29udGV4dCI6Imh0dHA6Ly93d3cudzMub3JnL25zL2FjdGl2aXR5c3RyZWFtcyIsInR5cGUiOiJDb2xsZWN0aW9uIiwibmFtZSI6IkFjdGl2aXR5IHN0cmVhbSIsInRvdGFsSXRlbXMiOjIsIml0ZW1zIjpbeyJ0eXBlIjoiQ3JlYXRlIiwicHVibGlzaGVkIjoiMjAxNS0xMC0wNlQxMToyMzo1MC4wMDBaIn0seyJ0eXBlIjoiQWRkIiwicHVibGlzaGVkIjoiMjAxNS0xMC0wNlQxMToyMzo0NS4wMDBaIn1dfQ.SsZ8kvPAbZmUO1PGQTkrElPDyoy8JXtSho0w-tFE0PGZzEVnVnRTRA-it86GUtf7kgFKLfGCtEK7sB5k6kS2GvHYqI-ZzioE9KU6uSd_6cjbwM7WYyYYjU7okDmM2eoc27QUV9eNDxDH5SyiiTug2YQ_3Iro64Ycn1WhRBaYBPvrotJ7wcWZDBq9GPncsn1sWrnVfKYyUgpe8YYWc6bCib1-fBtzuPZCh04rWC_H1cIM8rDRrtgaOdS8_G0v8uuBAqPezNNt2_Pu7MZLlcLeOz_45NzCUwh3_PpWfYC5--MoC3LH32hKFOhrbbuCONG48JwqrWPOEuM7BQrrtkd0ow"}}
               (http/get "OOPS_WRONG" anything) =throws=> (ConnectException.)))

       (fact "if the signed payload cannot be decoded then no activites are returned"
             (au/poll-activity-sources (dbh/create-in-memory-store) {:signed-activity-source {:url "signed-activity-source-url"}}) => []
             (provided
               (http/get "signed-activity-source-url" {:accept :json
                                                       :as     :json}) => {:body {:jku                "OOPS_WRONG"
                                                                                  :jws-signed-payload "NOT_A_VALID_ENCODED_PAYLOAD"}}))

       (fact "if the json message has invalid format then exception is thrown"
             (au/verify-and-return-activities ...signed-activity-source-url... ...jws...) => (throws Exception)
             (provided
               (au/verify-and-return-payload ...signed-activity-source-url... ...jws...) => "not-a-json")))

(facts "about validating format of the activities"
       (fact "activities that do not contain required attributes are filtered and logged"
             (let [invalid-activity {(keyword "@context") "http://www.w3.org/ns/activitystreams"
                                     :type                "Create"
                                     :actor               {:type "Person"
                                                           :name "Barry"}}
                   valid-activity {(keyword "@context") "http://www.w3.org/ns/activitystreams"
                                   :type                "Add"
                                   :published           "2015-08-03T14:49:38.407Z"
                                   :actor               {:type "Person"
                                                         :name "Joshua"}}
                   activities [invalid-activity valid-activity]]
               (au/remove-invalid-activities activities) => [valid-activity]
               (provided
                 (au/log-invalid-activity invalid-activity {:published :blank}) => anything))))

(fact "get-json-from-activity-source gracefully handles exceptions caused by bad/missing responses"
      (au/get-json-from-activity-source ...invalid-activity-src-url... nil) => nil
      (provided
        (http/get ...invalid-activity-src-url...
                  {:accept :json :as :json}) =throws=> (ConnectException.)))

(facts "sync activities retrieves activities from api and stores them"
       (let [an-activity-src-url "https://an-activity.src"
             another-activity-src-url "https://another-activity.src"
             json-src1 {(keyword "@context") "http://www.w3.org/ns/activitystreams"
                        :type                "Collection"
                        :name                "Activity stream"
                        :totalItems          2
                        :items               [{:actor     {:name "JDog"}
                                               :published ten-oclock
                                               :type      "a-type"}
                                              {:actor     {:name "KCat"}
                                               :published twelve-oclock
                                               :type      "another-type"}]}
             json-src2 {(keyword "@context") "http://www.w3.org/ns/activitystreams"
                        :type                "Collection"
                        :name                "Activity stream"
                        :totalItems          1
                        :items               [{:actor     {:name "LSheep"}
                                               :published eleven-oclock
                                               :type      "yet-another-type"}]}
             store (dbh/create-in-memory-store)]
         (facts "with stubbed activity retrieval"
                (against-background
                  (http/get an-activity-src-url anything) => {:body json-src1}
                  (http/get another-activity-src-url anything) => {:body json-src2})
                (fact "activities are stored"
                      (au/sync-activities! store {:an-activity-src      {:url an-activity-src-url}
                                                  :another-activity-src {:url another-activity-src-url}})
                      (count (activity/fetch-activities store)) => 3)
                (fact "activity types are stored"
                      (activity/fetch-activity-types store) => {:an-activity-src      ["a-type" "another-type"]
                                                                :another-activity-src ["yet-another-type"]}))))

(facts "retrieve-activities-from-source"
       (let [activity-url "https://an-activity.src"
             activity-source :test-activity-source]

         (fact "make get request with no extra params if no recent-published-time is found"
               (let [store (dbh/create-in-memory-store)]
                 (au/retrieve-activities-from-source store [activity-source {:url activity-url}])) => anything
               (provided
                 (http/get activity-url {:accept :json
                                         :as     :json}) => {}))

         (fact "make get request with most recent published time stored"
               (let [store (dbh/create-in-memory-store)
                     recent-activity {:activity-src activity-source
                                      :published    ten-oclock}
                     old-activity {:activity-src activity-source
                                   :published    nine-oclock}]
                 (activity/store-activity! store old-activity)
                 (activity/store-activity! store recent-activity)
                 (au/retrieve-activities-from-source store [activity-source {:url activity-url}])) => anything

               (provided
                 (http/get activity-url {:accept       :json
                                         :as           :json
                                         :query-params {:from ten-oclock}}) => {}))))
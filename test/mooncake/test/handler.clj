(ns mooncake.test.handler
  (:require [midje.sweet :refer :all]
            [clj-http.client :as http]
            [mooncake.handler :refer [index retrieve-activities load-activities]]))


(def ten-oclock "2015-01-01T10:00:00.000Z")
(def eleven-oclock "2015-01-01T11:00:00.000Z")
(def twelve-oclock "2015-01-01T12:00:00.000Z")

(fact "retrieve activities retrieves activities from multiple sources, sorts them by published time and assocs activity source into each activity"
      (let [an-activity-src-url "https://an-activity.src"
            another-activity-src-url "https://another-activity.src"]
        (retrieve-activities {:an-activity-src an-activity-src-url
                              :another-activity-src another-activity-src-url}) => [{:activity-src :an-activity-src
                                                                                    "actor" {"displayName" "KCat"}
                                                                                    "published" twelve-oclock}
                                                                                   {:activity-src :another-activity-src
                                                                                    "actor" {"displayName" "LSheep"}
                                                                                    "published" eleven-oclock}
                                                                                   {:activity-src :an-activity-src
                                                                                    "actor" {"displayName" "JDog"}
                                                                                    "published" ten-oclock}]
        (provided
          (http/get an-activity-src-url {:accept :json
                                         :as :json-string-keys})  => {:body [{"actor" {"displayName" "JDog"}
                                                                              "published" ten-oclock}
                                                                             {"actor" {"displayName" "KCat"}
                                                                              "published" twelve-oclock}]}
          (http/get another-activity-src-url {:accept :json
                                              :as :json-string-keys}) => {:body [{"actor" {"displayName" "LSheep"}
                                                                          "published" eleven-oclock}]})))

(fact "index handler displays activities retrieved from activity sources"
      (let [an-activity-src-url "https://an-activity.src"
            another-activity-src-url "https://another-activity.src"]
        (index {:context
                {:activity-sources
                 {:an-activity-src an-activity-src-url
                  :another-activity-src another-activity-src-url}}}) => (every-checker
                                                                          (contains {:status 200})
                                                                          (contains {:body (contains "JDog")})
                                                                          (contains {:body (contains "KCat")}))
        (provided
          (http/get an-activity-src-url {:accept :json
                                         :as :json-string-keys})       => {:body [{"actor" {"@type" "Person"
                                                                                            "displayName" "JDog"}
                                                                                   "published" ten-oclock}]}
          (http/get another-activity-src-url {:accept :json
                                              :as :json-string-keys})  => {:body [{"actor" {"@type" "Person"
                                                                                            "displayName" "KCat"}
                                                                                   "published" twelve-oclock}]})))
(fact "can load activity sources from a resource"
      (load-activities "test-activity-sources.yml") => {:test-activity-source-1 "https://test-activity.src/activities"
                                                        :test-activity-source-2 "https://another-test-activity.src"})

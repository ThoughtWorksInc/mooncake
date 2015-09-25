(ns mooncake.test.domain.activity
  (:require [midje.sweet :refer :all]
            [mooncake.domain.activity :as activity]))


(facts "about activity->action-text-key"
       (fact "is :objective for objectives"
             (let [activity {(keyword "@type") "Create"
                             :published        "2015-09-06T11:05:53+03:00"
                             :object           {(keyword "@type") "Objective"
                                                :displayName      "SAMPLE TITLE"
                                                :content          "SAMPLE CONTENT"}}]
               (activity/activity->action-text-key activity) => :objective))
       (fact "is :question for questions"
             (let [activity {(keyword "@type") "Question"
                             :published        "2015-09-06T11:05:53+03:00"
                             :object           {(keyword "@type") "Objective"
                                                :displayName      "SAMPLE TITLE"
                                                :content          "SAMPLE CONTENT"}}]
               (activity/activity->action-text-key activity) => :question))
       (fact "is :default for not recognised activity types"
             (let [activity {(keyword "@type") "Unrecognised"
                             :published        "2015-09-06T11:05:53+03:00"
                             :object           {(keyword "@type") "Cosmic X"
                                                :displayName      "SAMPLE TITLE"
                                                :content          "SAMPLE CONTENT"}}]
               (activity/activity->action-text-key activity) => :default)))

(fact "about activity->default-action-text"
      (let [activity {(keyword "@type") "Create"
                      :published        "2015-09-06T11:05:53+03:00"
                      :object           {(keyword "@type") "UnrecognisedType"
                                         :displayName      "SAMPLE TITLE"
                                         :content          "SAMPLE CONTENT"}}]
        (activity/activity->default-action-text activity) => "- UnrecognisedType - Create"))

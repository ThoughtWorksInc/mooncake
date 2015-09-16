(ns mooncake.test.domain.activity
  (:require [midje.sweet :refer :all]
            [mooncake.domain.activity :as activity]))


(facts "about activity->action-text-key"
       (fact "is :objective for objectives"
             (let [activity {"@type"     "Create"
                             "published" "2015-09-06T11:05:53+03:00"
                             "object"    {"@type"       "Objective"
                                          "displayName" "SAMPLE TITLE"
                                          "content"     "SAMPLE CONTENT"}}]
               (activity/activity->action-text-key activity) => :objective))
       (fact "is :question for questions"
             (let [activity {"@type"     "Question"
                             "published" "2015-09-06T11:05:53+03:00"
                             "object"    {"@type"       "Objective Question"
                                          "displayName" "SAMPLE TITLE"
                                          "content"     "SAMPLE CONTENT"}}]
               (activity/activity->action-text-key activity) => :question))
       (fact "is :default for not recognised activity types"
             (let [activity {"@type"     "Deleted"
                             "published" "2015-09-06T11:05:53+03:00"
                             "object"    {"@type"       "Cosmic X"
                                          "displayName" "SAMPLE TITLE"
                                          "content"     "SAMPLE CONTENT"}}]
               (activity/activity->action-text-key activity) => :default)))

(fact "about activity->default-action-text"
      (let [activity {"@type"     "Create"
                      "published" "2015-09-06T11:05:53+03:00"
                      "object"    {"@type"       "UnrecognisedType"
                                   "displayName" "SAMPLE TITLE"
                                   "content"     "SAMPLE CONTENT"}}]
        (activity/activity->default-action-text activity) => "- UnrecognisedType - Create"))

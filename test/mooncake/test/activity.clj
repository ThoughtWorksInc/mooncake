(ns mooncake.test.activity
  (:require [midje.sweet :refer :all]
            [clj-yaml.core :as yaml]
            [clojure.java.io :as io]
            [mooncake.activity :as a]
            [mooncake.test.test-helpers.db :as dbh]
            [mooncake.config :as config]))

(fact "can load activity sources from a resource"
      (a/load-activity-sources-from-resource "test-activity-sources.yml") => {:test-activity-source-1 {:url  "https://test-activity.src/activities"
                                                                                                       :name "Test Activity Source 1"
                                                                                                       :index 0}
                                                                              :test-activity-source-2 {:url  "https://another-test-activity.src"
                                                                                                       :name "Test Activity Source 2"
                                                                                                       :index 1}
                                                                              :test-activity-source-3 {:url  "https://yet-another-test-activity.src"
                                                                                                       :name "Test Activity Source 3"
                                                                                                       :index 2}})

(fact "can load activity sources from file"
      (let [source-data {:source-1 {:url "the-url" :name "the-name"}}
            expected-source-data (assoc-in source-data [:source-1 :index] 0)
            yaml (yaml/generate-string source-data)]
        (spit "test-activity-sources-file.yml" yaml)
        (a/load-activity-sources-from-file "test-activity-sources-file.yml") => expected-source-data
        (io/delete-file "test-activity-sources-file.yml")))

(fact "loading activity sources from resource"
      (let [source-data {:source-1 {:url "the-url" :name "the-name"}}
            expected-source-data (assoc-in source-data [:source-1 :index] 0)
            yaml (yaml/generate-string source-data)]
        (spit "test-activity-sources-file.yml" yaml)
        (fact "if no source file is specified, uses one configured in resources"
              (a/load-activity-sources {}) => (a/load-activity-sources-from-resource "activity-sources.yml"))
        (a/load-activity-sources {:activity-source-file "blah"}) => (throws Exception)
        (a/load-activity-sources {:activity-source-file "test-activity-sources-file.yml"}) => expected-source-data
        (io/delete-file "test-activity-sources-file.yml")))

(fact "can retrieve total number of activities queried"
      (let [store (dbh/create-in-memory-store)
            _ (dbh/create-dummy-activities store 60)
            feed-query [{:activity-src     "test-source"
                         (keyword "@type") ["Create"]}]]
        (a/total-count-by-feed store feed-query) => 60))

(tabular
  (fact "is-last-page returns true when on the last page"
        (a/is-last-page? ?page-number ?total-activities) => ?result)
  ?page-number  ?total-activities                       ?result
  1             (- config/activities-per-page 1)        true
  2             (+ config/activities-per-page 1)        true
  2             (+ 1 (* 2 config/activities-per-page))  false
  3             (* 3 config/activities-per-page)        true)


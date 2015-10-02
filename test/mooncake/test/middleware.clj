(ns mooncake.test.middleware
  (:require [midje.sweet :refer :all]
            [mooncake.test.test-helpers.enlive :as eh]
            [mooncake.test.test-helpers.db :as dbh]
            [mooncake.db.activity :as adb]
            [mooncake.middleware :as m]))

(defn example-handler [request]
  "return value")

(defn wrap-function [handler]
  (fn [request] "wrap function return value"))

(def handlers {:handler-1 example-handler
               :handler-2 example-handler
               :handler-3 example-handler})

(facts "about wrap-handlers-excluding"
       (fact "wraps all handlers in a wrap-function"
             (let [wrapped-handlers (m/wrap-handlers-excluding handlers wrap-function nil)]
               ((:handler-1 wrapped-handlers) "request") => "wrap function return value"
               ((:handler-2 wrapped-handlers) "request") => "wrap function return value"
               ((:handler-3 wrapped-handlers) "request") => "wrap function return value"))

       (fact "takes a set of exclusions which are not wrapped"
             (let [wrapped-handlers (m/wrap-handlers-excluding handlers wrap-function #{:handler-1 :handler-3})]
               ((:handler-1 wrapped-handlers) "request") => "return value"
               ((:handler-2 wrapped-handlers) "request") => "wrap function return value"
               ((:handler-3 wrapped-handlers) "request") => "return value")))

(facts "wrap-just-these-handlers only wraps the specified handlers"
       (let [wrapped-handlers (m/wrap-just-these-handlers handlers wrap-function #{:handler-2 :handler-3})]
         ((:handler-1 wrapped-handlers) "request") => "return value"
         ((:handler-2 wrapped-handlers) "request") => "wrap function return value"
         ((:handler-3 wrapped-handlers) "request") => "wrap function return value"))

(fact "wrap-activity-sources-and-types"
      (let [db (dbh/create-in-memory-db)
            activity-sources {:activity-src         {:name "A. Activity Source" :url "some url"}
                              :another-activity-src {:name "B. Another Source" :url "another url"}}

            _ (adb/update-activity-types-for-activity-source! db "activity-src" "Create")
            _ (adb/update-activity-types-for-activity-source! db "activity-src" "Question")
            _ (adb/update-activity-types-for-activity-source! db "another-activity-src" "Question")
            handler-which-returns-request (fn [request] request)
            wrapped-handler (m/wrap-activity-sources-and-types db activity-sources handler-which-returns-request)
            wrapped-request (wrapped-handler {})]
        wrapped-request => {:context {:activity-sources {:activity-src         {:name           "A. Activity Source"
                                                                                :url            "some url"
                                                                                :activity-types ["Create" "Question"]}
                                                         :another-activity-src {:name           "B. Another Source"
                                                                                :url            "another url"
                                                                                :activity-types ["Question"]}}}}))

(fact "renders 403 error page when response status is 403"
      (let [handler-that-always-403s (fn [req] {:status 403})
            stub-error-403-handler (fn [req] ...error-403-response...)
            wrapped-handler (m/wrap-handle-403 handler-that-always-403s stub-error-403-handler)]
        (wrapped-handler ...request...) => ...error-403-response...))

(facts "about wrap-signed-in"
       (let [handler (fn [request] ...handled...)
             wrapped-handler (m/wrap-signed-in handler "/sign-in")]

         (fact "calls wrapped handler when user is signed in"
               (wrapped-handler {:session {:username ...username...}}) => ...handled...)

         (fact "redirects to provided route when user is not signed in"
               (wrapped-handler {}) => (eh/check-redirects-to "/sign-in"))))

(ns mooncake.integration.kerodon
  (:require [midje.sweet :refer :all]
            [kerodon.core :as k]
            [ring.adapter.jetty :as ring-jetty]
            [stonecutter-oauth.client :as soc]
            [mooncake.routes :as routes]
            [mooncake.handler :as h]
            [mooncake.config :as c]
            [mooncake.integration.kerodon-helpers :as kh]
            [mooncake.integration.kerodon-selectors :as ks]))

(defn print-enlive [state]
  (prn (-> state :enlive))
  state)

(defn print-request [state]
  (prn (-> state :request))
  state)

(defn print-state [state]
  (prn state)
  state)


(def server (atom nil))
(defn start-server [] (swap! server (fn [_] (ring-jetty/run-jetty h/app {:host "127.0.0.1" :port 3000 :join? false}))))
(defn stop-server [] (.stop @server))

(background
  (soc/request-access-token! anything anything) => {:user-id "test-stonecutter-user-uuid"})

(future-facts "The index page redirects to /sign-in when user is not signed in"
       (-> (k/session h/app)
           (k/visit "/")
           (kh/check-and-follow-redirect "sign-in")
           (kh/page-uri-is "/sign-in")
           (kh/response-status-is 200)))

(facts "Signing in redirects to the index page"
       (-> (k/session h/app)
           (k/visit (routes/absolute-path (c/create-config) :stonecutter-callback))
           (kh/check-and-follow-redirect)
           (kh/page-uri-is "/")
           (kh/response-status-is 200)))

(against-background
  [(before :contents (start-server))
   (after :contents (stop-server))]
  (facts "Stub activities are rendered"
         (-> (k/session (h/create-app (c/create-config) {:stub-activity-source "http://127.0.0.1:3000/stub-activities"}))
             (k/visit "/d-cent-callback")
             (k/visit "/")
             (kh/page-uri-is "/")
             (kh/response-status-is 200)
             (kh/selector-exists [ks/index-page-body])
             (kh/selector-includes-content [ks/index-page-activity-item-title] "Stub activity title")
             (kh/selector-includes-content [ks/index-page-activity-item-action] "Barry - STUB_ACTIVITY - Create")
             (kh/selector-has-attribute-with-content [ks/index-page-activity-item-link] :href "http://stub-activity.url"))))

(facts "Invalid activity source responses are handled gracefully"
       (-> (k/session (h/create-app (c/create-config) {:invalid-activity-src  "http://localhost:6666/not-an-activity-source"}))
           (k/visit "/d-cent-callback")
           (k/visit "/")
           (kh/page-uri-is "/")
           (kh/response-status-is 200)))

(facts "Going to an unknown uri renders the 404 page"
       (-> (k/session h/app)
           (k/visit "/not-a-valid-uri")
           (kh/page-uri-is "/not-a-valid-uri")
           (kh/response-status-is 404)
           (kh/selector-exists [ks/error-404-page-body])))

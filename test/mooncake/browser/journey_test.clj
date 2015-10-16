(ns mooncake.browser.journey-test
  (:require [midje.sweet :refer :all]
            [clj-webdriver.taxi :as wd]
            [clojure.test :refer :all]
            [ring.adapter.jetty :refer [run-jetty]]
            [mooncake.test.test-helpers.db :as dbh]
            [mooncake.handler :as h]
            [mooncake.integration.kerodon :as kero]
            [mooncake.activity :as a]))

(def localhost "localhost:5439")

(defn wait-for-selector [selector]
  (try
    (wd/wait-until #(not (empty? (wd/css-finder selector))) 5000)
    (catch Exception e
      (prn (str ">>>>>>>>>> Selector could not be found: " selector))
      (prn "==========  PAGE SOURCE ==========")
      (prn (wd/page-source))
      (prn "==========  END PAGE SOURCE ==========")
      (throw e))))

(def test-port 5439)

(def mooncake-feed-body ".func--feed-page")

(def stub-config-map
  {:port          5439
   :host          "localhost"
   :base-url      "http://localhost:5439"
   :secure        "false"
   :client-id     "fake stonecutter client id"
   :client-secret "fake stonecutter client secret"
   :auth-url      "fake stonecutter auth url"
   :mongo-uri     "mongodb://localhost:27017/mooncake-dev"
   :stub-user     "MRS STUBBY"})

(def test-store
  (atom (dbh/create-in-memory-store)))

(defn start-server []
  (let [activity-sources (a/load-activity-sources-from-resource "test-activity-sources.yml")
        app-routes (h/create-app stub-config-map @test-store activity-sources)]
    (loop [server (run-jetty app-routes {:port test-port :host "localhost" :join? false})]
      (if (.isStarted server)
        server
        (recur server)))))

(defn stop-server [server]
  (.stop server))

(defn start-browser []
  (wd/set-driver! {:browser :firefox}))

(defn stop-browser []
  (wd/quit))

(def server (atom {}))

(defn count-activity-items []
  (count (wd/elements ".func--activity-item")))

(against-background
  [(before :contents (do (reset! server (start-server))
                         (kero/create-dummy-activities @test-store 230)
                         (start-browser)))
   (after :contents (do
                      (stop-browser)
                      (stop-server @server)))]

  (try
    (facts "feedpage loads more activities when load more is triggered" :browser
           (wd/to (str localhost "/d-cent-sign-in"))
           (wd/current-url) => (contains (str localhost "/"))
           (wait-for-selector mooncake-feed-body)
           (count-activity-items) => 50
           ;(wd/click ".func--load-activities__link") todo 16/10/2015 - NE + CW | 228 | browser test for JS hot loading of feed activities
           ;(count-activity-items) => 100
           )


    (catch Exception e
      (throw e))))
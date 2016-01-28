(defproject mooncake "0.1.0-SNAPSHOT"
  :description "A D-CENT project: Secure notifications combined with w3 activity streams"
  :url "https://mooncake-staging.herokuapp.com/"
  :min-lein-version "2.0.0"
  :license "The MIT License"
  :dependencies [[org.clojure/clojure "1.7.0"]
                 [ring/ring-defaults "0.1.5"]
                 [ring/ring-jetty-adapter "1.4.0"]
                 [scenic "0.2.5"]
                 [enlive "1.1.6"]
                 [org.clojure/tools.logging "0.3.1"]
                 [com.taoensso/tower "3.0.2"]
                 [traduki "0.1.3-SNAPSHOT"]
                 [clj-yaml "0.4.0"]
                 [cljsjs/moment "2.10.6-1"]
                 [cheshire "5.5.0"]
                 [clj-http "2.0.0"]
                 [org.clojars.d-cent/stonecutter-oauth "0.2.0-SNAPSHOT"]
                 [environ "1.0.1"]
                 [jarohen/chime "0.1.6"]
                 [com.novemberain/monger "2.1.0"]
                 [hickory "0.5.4"]
                 [ragtime "0.4.2"]
                 [log4j/log4j "1.2.17" :exclusions [javax.mail/mail
                                                    javax.jms/jms
                                                    com.sun.jdmk/jmxtools
                                                    com.sun.jmx/jmxri]]
                 [org.clojure/clojurescript "0.0-3308" :scope "provided"]
                 [prismatic/dommy "1.1.0"]
                 [cljs-ajax "0.3.14"]
                 [ring/ring-json "0.4.0"]
                 [org.bitbucket.b_c/jose4j "0.4.4"]
                 [org.slf4j/slf4j-simple "1.7.12"]]
  :main mooncake.handler
  :profiles {:dev     {:dependencies   [[ring-mock "0.1.5"]
                                        [midje "1.7.0"]
                                        [clj-webdriver "0.7.2" :exclusions [org.clojure/core.cache
                                                                            org.seleniumhq.selenium/selenium-java
                                                                            org.seleniumhq.selenium/selenium-server
                                                                            org.seleniumhq.selenium/selenium-remote-driver
                                                                            xml-apis]]
                                        [xml-apis "1.4.01"]
                                        [org.seleniumhq.selenium/selenium-server "2.45.0"]
                                        [org.seleniumhq.selenium/selenium-java "2.45.0"]
                                        [org.seleniumhq.selenium/selenium-remote-driver "2.45.0"]
                                        [kerodon "0.6.1"]
                                        [com.cognitect/transit-cljs "0.8.225"]]
                       :plugins        [[lein-environ "1.0.0"]
                                        [lein-midje "3.1.3"]
                                        [lein-ancient "0.6.7"]
                                        [lein-cljsbuild "1.0.6"]
                                        [lein-shell "0.4.1"]
                                        [com.cemerick/clojurescript.test "0.3.3"]]
                       :resource-paths ["resources" "test-resources"]
                       :env            {:secure        "false"
                                        :client-id     "fake stonecutter client id"
                                        :client-secret "fake stonecutter client secret"
                                        :auth-url      "fake stonecutter auth url"
                                        :mongo-uri     "mongodb://localhost:27017/mooncake-dev"
                                        :polling-interval "1000"}
                       :aliases        {"cljs-build"      ["cljsbuild" "once" "prod"]
                                        "cljs-test"       ["cljsbuild" "test"]
                                        "test"            ["do" "clean," "gulp," "cljs-test," "cljs-build," "midje" ]
                                        "test-cljs"       ["do" "clean," "gulp," "cljs-test"]
                                        "auto-cljs"       ["do" "test-cljs," "cljsbuild" "auto" "test"]
                                        "browser"         ["do" "clean," "gulp," "cljs-build," "midje" "mooncake.browser.*"]
                                        "gulp"            ["shell" "npm" "run" "gulp" "--" "build"]
                                        "stub"            ["do" "gulp," "cljs-build," "with-profile" "dev,stub" "run"]
                                        "auto-no-browser" ["test" ":autotest" "src/" "test/mooncake/test/" "test/mooncake/integration/"]}
                       :cljsbuild      {:builds [{:id           "prod"
                                                  :source-paths ["src-cljs"]
                                                  :compiler     {:output-to     "resources/public/js/main.js"
                                                                 :asset-path    "js/out"
                                                                 :optimizations :advanced
                                                                 :pretty-print  false}}
                                                 {:id             "test"
                                                  :source-paths   ["src" "src-cljs" "test-cljs"]
                                                  :compiler       {:output-to     "target/cljs/testable.js"
                                                                   :optimizations :whitespace
                                                                   :pretty-print  true}}]
                                        :test-commands {"phantom" ["phantomjs" :runner "target/cljs/testable.js"]}}}
             :stub    {:env {:stub-user "MRS STUBBY"}}
             :uberjar {:hooks       [leiningen.cljsbuild]
                       :env         {:production true}
                       :aot         :all
                       :omit-source true
                       :cljsbuild   {:jar    true
                                     :builds [{:source-paths ["src-cljs"]
                                               :compiler     {:output-to     "resources/public/js/main.js"
                                                              :optimizations :advanced
                                                              :pretty-print  false}}]}}}
  )
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
                 [traduki "0.1.3-SNAPSHOT"]
                 [clj-yaml "0.4.0"]
                 [cheshire "5.5.0"]
                 [bilus/clojure-humanize "0.1.0"]
                 [clj-http "2.0.0"]
                 [org.clojars.d-cent/stonecutter-oauth "0.2.0-SNAPSHOT"]
                 [environ "1.0.1"]
                 [jarohen/chime "0.1.6"]
                 [com.novemberain/monger "2.1.0"]
                 [log4j/log4j "1.2.17" :exclusions [javax.mail/mail
                                                    javax.jms/jms
                                                    com.sun.jdmk/jmxtools
                                                    com.sun.jmx/jmxri]]
                 [org.clojure/clojurescript "0.0-3308" :scope "provided"]
                 [prismatic/dommy "1.1.0"]]
  :main mooncake.handler
  :profiles {:dev     {:dependencies   [[ring-mock "0.1.5"]
                                        [midje "1.7.0"]
                                        [kerodon "0.6.1"]]
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
                                        :mongo-uri     "mongodb://localhost:27017/mooncake-dev"}
                       :aliases        {"stub"       ["with-profile" "dev,stub" "run"]
                                        "cljs-build" ["cljsbuild" "once" "prod"]
                                        "cljs-test"  ["cljsbuild" "once" "test"]
                                        "test"       ["do" "clean," "gulp," "midje," "cljs-test"]
                                        "test-cljs"  ["do" "clean," "gulp," "cljs-test"]
                                        "auto-cljs"  ["do" "test-cljs," "cljsbuild" "auto" "test"]
                                        "gulp"       ["shell" "gulp" "build"]
                                        "start"      ["do" "gulp," "cljs-build," "run"]}
                       :cljsbuild      {:builds [{:id           "prod"
                                                  :source-paths ["src-cljs"]
                                                  :compiler     {:output-to     "resources/public/js/main.js"
                                                                 :asset-path    "js/out"
                                                                 :optimizations :advanced
                                                                 :pretty-print  false}}
                                                 {:id             "test"
                                                  :source-paths   ["src-cljs" "test-cljs"]
                                                  :notify-command ["phantomjs" :cljs.test/runner "target/cljs/testable.js"]
                                                  :compiler       {:output-to     "target/cljs/testable.js"
                                                                   :optimizations :whitespace
                                                                   :pretty-print  true}}]}}
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
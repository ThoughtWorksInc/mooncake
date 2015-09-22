(defproject mooncake "0.1.0-SNAPSHOT"
  :description "A D-CENT project: Secure notifications combined with w3 activity streams"
  :url "https://mooncake-staging.herokuapp.com/"
  :min-lein-version "2.0.0"
  :license "The MIT License"
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [ring/ring-defaults "0.1.5"]
                 [ring/ring-jetty-adapter "1.4.0"]
                 [scenic "0.2.3"]
                 [enlive "1.1.6"]
                 [org.clojure/tools.logging "0.3.1"]
                 [traduki "0.1.3-SNAPSHOT"]
                 [clj-yaml "0.4.0"]
                 [cheshire "5.5.0"]
                 [bilus/clojure-humanize "0.1.0"]
                 [clj-http "2.0.0"]
                 [org.clojars.d-cent/stonecutter-oauth "0.2.0-SNAPSHOT"]
                 [environ "1.0.0"]
                 [jarohen/chime "0.1.6"]
                 [com.novemberain/monger "2.1.0"]
                 [log4j/log4j "1.2.17" :exclusions [javax.mail/mail
                                                    javax.jms/jms
                                                    com.sun.jdmk/jmxtools
                                                    com.sun.jmx/jmxri]]]
  :main mooncake.handler
  :profiles {:dev     {:dependencies   [[ring-mock "0.1.5"]
                                        [midje "1.7.0"]
                                        [kerodon "0.6.1"]]
                       :plugins        [[lein-environ "1.0.0"]
                                        [lein-midje "3.1.3"]
                                        [lein-ancient "0.6.7"]]
                       :resource-paths ["resources" "test-resources"]
                       :env            {:secure        "false"
                                        :client-id     "fake stonecutter client id"
                                        :client-secret "fake stonecutter client secret"
                                        :auth-url      "fake stonecutter auth url"
                                        :mongo-uri     "mongodb://localhost:27017/mooncake-dev"}}
             :stub    {:env {:stub-user "MRS STUBBY"}}
             :uberjar {:aot :all}}
  :aliases {"stub" ["with-profile" "dev,stub" "run"]}
  )
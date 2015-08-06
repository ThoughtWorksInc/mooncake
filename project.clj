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
                 [traduki "0.1.1-SNAPSHOT"]
                 [clj-yaml "0.4.0"]
                 [cheshire "5.5.0"]
                 [bilus/clojure-humanize "0.1.0"]
                 [clj-http "2.0.0"]
                 [environ "1.0.0"]]
  :main mooncake.handler
  :target-path "target/%s"
  :profiles {:dev {:dependencies   [[ring-mock "0.1.5"]
                                    [midje "1.7.0"]
                                    [kerodon "0.6.1"]]
                   :plugins        [[lein-environ "1.0.0"]
                                    [lein-midje "3.1.3"]
                                    [lein-ancient "0.6.7"]]
                   :jvm-opts ["-Dlog4j.configuration=log4j.dev"]
                   :env {:secure "false"}}
             :uberjar {:aot :all}})

(defproject mooncake "0.1.0-SNAPSHOT"
  :description "D-CENT project: a secure notifications system"
  :url "TODO"
  :license "The MIT License"
  :dependencies [[org.clojure/clojure "1.6.0"]]
  :main ^:skip-aot mooncake.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})

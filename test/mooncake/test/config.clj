(ns mooncake.test.config
  (:require [midje.sweet :refer :all]
            [mooncake.config :refer [secure?]]))


(tabular
  (fact "secure? is true by default"
        (secure? {:secure ?secure-env-value}) => ?return-value)
  ?secure-env-value     ?return-value
  "true"                true
  "asdf"                true
  ""                    true
  nil                   true
  "false"               false)

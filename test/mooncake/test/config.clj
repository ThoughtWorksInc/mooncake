(ns mooncake.test.config
  (:require [midje.sweet :refer :all]
            [mooncake.config :as c]))


(fact "get-env throws an exception when the requested key isn't in the env-vars set"
      (c/get-env {:env-key "env-var"} :some-key-that-isnt-in-env-vars) => (throws Exception))

(tabular
  (fact "secure? is true by default"
        (c/secure? {:secure ?secure-env-value}) => ?return-value)
  ?secure-env-value     ?return-value
  "true"                true
  "asdf"                true
  ""                    true
  nil                   true
  "false"               false)

(fact "can get sync-interval as integer"
      (c/sync-interval {}) => 60
      (c/sync-interval {:sync-interval "30"}) => 30)

(fact "feature toggle for js-loading is always false"
      c/js-loading-feature? => false)
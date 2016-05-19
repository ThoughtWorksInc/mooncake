(ns mooncake.test.validation
  (:require [midje.sweet :refer :all]
            [mooncake.validation :as v]))

(defn string-of-length [n]
  (apply str (repeat n "x")))

(tabular
  (fact "validating username"
        (v/validate-username ?username (fn [_] ?is-duplicate?)) => ?validations)
  ?username                 ?is-duplicate?               ?validations
  "Timmy"                   false                        nil
  nil                       false                        {:username :blank}
  ""                        false                        {:username :blank}
  "          "              false                        {:username :blank}
  "\t\t\t\t\t\t\t\t"        false                        {:username :blank}
  (string-of-length 16)     false                        nil
  (string-of-length 17)     false                        {:username :too-long}
  "invalid space"           false                        {:username :invalid-format}
  "invalid-hyphen"          false                        {:username :invalid-format}
  "invalid@symbol"          false                        {:username :invalid-format}
  "Timmy"                   true                         {:username :duplicate})

(tabular
  (fact "validating incoming activity"
        (v/validate-activity ?activity) => ?validations)
  ?activity                                ?validations
  {:published "2015-08-03T14:49:38.407Z"
   :type "Add"}                            nil
  {}                                       {:published :blank
                                            :type :blank}
  {:published "invalid-timestamp"}         {:published :invalid
                                            :type :blank}
  {:published        "    "
   :type "    "}                           {:published :blank
                                            :type :blank}
  {:published "\t\t\t\t\t\t\t\t"
   :type "\t\t\t\t\t"}                     {:published :blank
                                            :type :blank}
  {:published "invalid-timestamp"
   :type "Add"}                            {:published :invalid})

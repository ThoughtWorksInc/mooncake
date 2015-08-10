(ns mooncake.test.test-helpers
  (:require [midje.sweet :as midje]))

(defn check-redirects-to [path]
  (midje/checker [response] (and
                        (= (:status response) 302)
                        (= (get-in response [:headers "Location"]) path))))

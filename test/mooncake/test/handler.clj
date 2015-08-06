(ns mooncake.test.handler
  (:require [midje.sweet :refer :all]
            [clj-http.client :as http]
            [mooncake.handler :refer [index]]))

(fact "index handler makes a get request to objective8 activities"
      (index {}) => (every-checker (contains {:status 200})
                                   (contains {:body (contains "JDog")}))
      (provided
        (http/get "https://objective8.dcentproject.eu/activities" {:accept :json})  => {:body "[{\"actor\":
                                                                                              {\"@type\": \"Person\",
                                                                                              \"displayName\": \"JDog\"}}]"}))

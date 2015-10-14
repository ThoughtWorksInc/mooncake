(ns mooncake.test.translation
  (:require [midje.sweet :refer :all]
            [mooncake.translation :as t]))

(facts "about getting locale from requests"
       (fact "request with no locales set defaults to :en"
             (t/get-locale-from-request {}) => :en)
       (fact "request with session locale set, always take session locale above others"
             (t/get-locale-from-request {:session {:locale :fi} :locale :en}) => :fi)
       (fact "request can take locale if no session locale is set"
             (t/get-locale-from-request {:session {:locale nil} :locale :fr}) => :fr))
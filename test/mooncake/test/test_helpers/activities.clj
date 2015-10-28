(ns mooncake.test.test-helpers.activities)

(def activity-JDog {:displayName      "JDog"
                         :published        "2015-08-12T00:00:00.000Z"
                         :activity-src     "source-1"
                         (keyword "@type") "Create"})

(def activity-KCat {:displayName      "KCat"
                    :published        "2015-08-12T00:00:01.000Z"
                    :activity-src     "source-2"
                    (keyword "@type") "Create"})

(def json-for-JDog "{
  \"displayName\" : \"JDog\",
  \"published\" : \"2015-08-12T00:00:00.000Z\",
  \"activity-src\" : \"source-1\",
  \"@type\" : \"Create\",
  \"activity-src-no\" : 1,
  \"formatted-time\" : \"2 months ago\",
  \"action-text\" : \"--Create\",
  \"limited-title\" : null}")

(def json-for-KCat "{
    \"displayName\" : \"KCat\",
    \"published\" : \"2015-08-12T00:00:01.000Z\",
    \"activity-src\" : \"source-2\",
    \"@type\" : \"Create\",
    \"activity-src-no\" : 0,
    \"formatted-time\" : \"2 months ago\",
    \"action-text\" : \"--Create\",
    \"limited-title\" : null}")


(ns mooncake.test.view.view-helpers
  (:require [midje.sweet :refer :all]
            [mooncake.view.view-helpers :as vh]))

(facts "about limit-characters"
       (fact "does not modify input text is shorter than threshold"
             (vh/limit-characters 16 "foo") => "foo")
       (fact "removes characters over the given threshold"
             (vh/limit-characters 16 "foo bar foo baar and something") => "foo bar foo baar")


       (tabular
         (fact "removes charactes starting with last word which exceeded the given character threshold"
             (vh/limit-characters 16 (str "foo bar foo" ?separator "baaaar and something")) => "foo bar foo")

         ?separator
         " "
         ","
         "."
         ":"
         ";"
         "("
         ")"
         "   "
         ", "
         ".   "
         )
       (fact "does not break hyphenated words"
             (vh/limit-characters 16 "foo bar foo-baaaar and something") => "foo bar")
       (fact "allows hyphen at the end of the last word"
             (vh/limit-characters 16 "foo bar foooao- badsaaar and something") => "foo bar foooao-")
       (fact "removes characters exactly after threshold if text has no line breaks before threshold"
             (vh/limit-characters 16 "foodbarcfoo-baaaareand something") => "foodbarcfoo-baaa")
       (fact "returns empty string if input text is empty"
             (vh/limit-characters 16 "") => "")
       (fact "returns nil if input text is nil"
             (vh/limit-characters 16 nil) => nil))

(facts "about limit-text-length-if-above"
      (fact "does not add ellipsis character if text is shorter than the given threshold"
            (vh/limit-text-length-if-above 16 "foo bar") => "foo bar")
       (fact "adds ellipsis character if text is longer than the given threshold"
             (vh/limit-text-length-if-above 16 "foo bar foo baar and something") => "foo bar foo baar&hellip;")
       (fact "returns nil if input text is nil"
             (vh/limit-text-length-if-above 16 nil) => nil))
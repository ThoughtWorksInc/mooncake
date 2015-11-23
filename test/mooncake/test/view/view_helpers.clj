(ns mooncake.test.view.view-helpers
  (:require [midje.sweet :refer :all]
            [mooncake.view.view-helpers :as vh]
            [mooncake.test.test-helpers.enlive :as eh]
            [net.cgrand.enlive-html :as html]
            [mooncake.view.create-account :as ca]
            [mooncake.view.customise-feed :as cf]
            [mooncake.view.sign-in :as si]
            [mooncake.view.feed :as feed]
            [mooncake.view.error :as e]))

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
       (fact "includes line breaks in result"
             (vh/limit-characters 16 "foo\nbar\nfo bar ddaaar and something") => "foo\nbar\nfo bar")
       (fact "returns empty string if input text is empty"
             (vh/limit-characters 16 "") => "")
       (fact "returns nil if input text is nil"
             (vh/limit-characters 16 nil) => nil))

(facts "about limit-text-length-if-above"
       (fact "does not add ellipsis character if text is shorter than the given threshold"
             (vh/limit-text-length-if-above 16 "foo bar") => "foo bar")
       (fact "adds ellipsis character if text is longer than the given threshold"
             (vh/limit-text-length-if-above 16 "foo bar foo baar and something") => "foo bar foo baar\u2026")
       (fact "returns nil if input text is nil"
             (vh/limit-text-length-if-above 16 nil) => nil))

(facts "about updating location settings"
       (fact "can update html lang attribute"
             (let [template (vh/load-template "public/create-account.html")
                   updated-template (vh/update-language template "fi")]
               updated-template => (eh/has-attr? [:html] :lang "fi"))))

(tabular
  (fact "about setting the language attribute based on the request language"
        (let [session-m  {:locale :fi}
              context {:params {} :error-m {} :session session-m}]
          (first (html/select (?page context) [:html])) => (contains {:attrs (contains {:lang :fi})})))
  ?page
  ca/create-account
  cf/customise-feed
  e/internal-server-error
  feed/feed
  si/sign-in
  )
(ns mooncake.test.view.feed
  (:require [midje.sweet :refer :all]
            [net.cgrand.enlive-html :as html]
            [clj-time.core :as c]
            [clj-time.format :as f]
            [mooncake.routes :as routes]
            [mooncake.test.test-helpers.enlive :as eh]
            [mooncake.view.feed :as fv]))

(fact "feed page should return feed template"
      (let [page (fv/feed :request)]
        page => (eh/has-class? [:body] "func--feed-page")))

(eh/test-translations "feed page" fv/feed)
(eh/test-logo-link fv/feed)

(fact "username is rendered"
      (fv/feed {:session {:username "Dave"}}) => (eh/text-is? [:.clj--username] "Dave"))

(fact "sign-out link is rendered and directs to /sign-out when user is signed in"
      (let [page (fv/feed {:session {:username ...username...}})]
        page => (eh/links-to? [:.clj--sign-out__link] (routes/path :sign-out))
        page =not=> (eh/has-class? [:.clj--sign-out__link] "clj--STRIP")))

(fact "sign-out link is not rendered if user is not signed in"
      (let [page (fv/feed {})]
        page => (eh/has-class? [:.clj--sign-out__link] "clj--STRIP")))

(fact "customise-feed link is rendered and directs to /customise-feed when user is signed in"
      (let [page (fv/feed {:session {:username ...username...}})]
        page => (eh/links-to? [:.clj--customise-feed__link] (routes/path :show-customise-feed))
        page =not=> (eh/has-class? [:.clj--customise-feed__link] "clj--STRIP")))

(fact "customise-feed link is not rendered if user is not signed in"
      (let [page (fv/feed {})]
        page => (eh/has-class? [:.clj--customise-feed__link] "clj--STRIP")))

(tabular
  (fact "newer and older links only display on relevant pages"
        (let [page (fv/feed {:context {:is-last-page ?is-last-page}
                             :params  {:page-number ?page-number}})]
          page ?newer-link-hidden (eh/has-class? [:.clj--newer-activities__link] "clj--STRIP")
          page ?older-link-hidden (eh/has-class? [:.clj--older-activities__link] "clj--STRIP")))

  ?page-number ?is-last-page ?newer-link-hidden ?older-link-hidden
  1 false => =not=>
  2 true =not=> =>
  1 true => =>
  3 false =not=> =not=>)

(fact "activities are rendered on the page"
      (let [ten-minutes-ago (-> -10 c/minutes c/from-now)
            ten-minutes-ago-iso (f/unparse (f/formatters :date-time) ten-minutes-ago)
            ten-minutes-ago-custom (f/unparse fv/time-formatter ten-minutes-ago)
            page (fv/feed {:context
                           {:activities
                            [{:activity-src        "an-objective8-activity-src"
                              (keyword "@context") "http://www.w3.org/ns/activitystreams"
                              :type                "Create"
                              :published           ten-minutes-ago-iso
                              :actor               {:type        "Person"
                                                    :displayName "JDog"}
                              :object              {:type        "Objective"
                                                    :displayName "OBJECTIVE 7 TITLE"
                                                    :content     "We want to establish Activity Types for Objective8"
                                                    :url         "http://objective8.dcentproject.eu/objectives/7"}
                              :relInsertTime       "6"}
                             {:activity-src        "a-helsinki-activity-src"
                              (keyword "@context") "http://www.w3.org/ns/activitystreams"
                              :type                "Add"
                              :published           "2015-09-06T11:05:53.002Z"
                              :actor               {:type           "Group"
                                                    :displayName    "Kaupunginjohtaja/J"
                                                    (keyword "@id") "http://dev.hel.fi/paatokset/v1/policymaker/50/"}
                              :object              {(keyword "@id") "http://dev.hel.fi/paatokset/v1/agenda_item/52359/"
                                                    :displayName    "Ymp\u00e4rist\u00f6raportoinnin asiantuntijaty\u00f6ryhm\u00e4n asettaminen toimikaudeksi 2015\u20132020"
                                                    :type           "Content"
                                                    :url            "http://dev.hel.fi/paatokset/asia/hel-2015-005343/11010vh1j-2015-25/"
                                                    :content        "some Finnish HTML"}
                              :target              {:displayName "Ymp\u00e4rist\u00f6raportoinnin asiantuntijaty\u00f6ryhm\u00e4n asettaminen toimikaudeksi 2015\u20132020"
                                                    :content     "some Finnish HTML"}
                              :relInsertTime       "9"}
                             {:activity-src        "another-objective8-activity-src"
                              (keyword "@context") "http://www.w3.org/ns/activitystreams"
                              :type                "Question"
                              :published           "2015-08-04T14:49:38.407Z"
                              :actor               {:type "Person"
                                                    :name "Lala"}
                              :object              {:type        "Objective Question"
                                                    :name        "QUESTION 6 TITLE"
                                                    :description "Yes."
                                                    :url         "http://objective8.dcentproject.eu/objectives/6/questions/23"}
                              :target              {:name "OBJECTIVE 6 TITLE"
                                                    :type "Objective"
                                                    :url  "http://objective8.dcentproject.eu/objectives/6"}
                              :relInsertTime       "3"}]}})
            [first-activity-item second-activity-item third-activity-item] (html/select page [:.clj--activity-item])]

        (count (html/select page [:.clj--activity-item])) => 3

        first-activity-item => (eh/links-to? [:.clj--activity-item__link] "http://objective8.dcentproject.eu/objectives/7")
        first-activity-item => (eh/has-attr? [:.clj--activity-item__time] :datetime ten-minutes-ago-iso)
        first-activity-item => (eh/text-is? [:.clj--activity-item__time] ten-minutes-ago-custom)
        first-activity-item => (eh/text-is? [:.clj--activity-item__action__author] "JDog")
        first-activity-item => (eh/has-attr? [:.clj--activity-item__action] :data-l8n "content:activity-type/action-text-objective")
        first-activity-item => (eh/text-is? [:.clj--activity-item__title] "OBJECTIVE 7 TITLE")
        first-activity-item => (eh/has-attr? [:.clj--activity-item__id] :hidden "hidden")
        first-activity-item => (eh/text-is? [:.clj--activity-item__id] "6")

        second-activity-item => (eh/links-to? [:.clj--activity-item__link] "http://dev.hel.fi/paatokset/asia/hel-2015-005343/11010vh1j-2015-25/")
        second-activity-item => (eh/has-attr? [:.clj--activity-item__time] :datetime "2015-09-06T11:05:53.002Z")
        second-activity-item => (eh/text-is? [:.clj--activity-item__time] "2015-09-06 11:05:53")
        second-activity-item => (eh/text-is? [:.clj--activity-item__action__author] "Kaupunginjohtaja/J")
        second-activity-item => (eh/text-is? [:.clj--activity-item__action] "- Content - Add")
        second-activity-item => (eh/text-is? [:.clj--activity-item__connector] " -")
        second-activity-item => (eh/text-is? [:.clj--activity-item__title] "Ymp\u00e4rist\u00f6raportoinnin asiantuntijaty\u00f6ryhm\u00e4n asettaminen toimikaudeksi 2015\u20132020")
        second-activity-item => (eh/has-attr? [:.clj--activity-item__id] :hidden "hidden")
        second-activity-item => (eh/text-is? [:.clj--activity-item__id] "9")

        third-activity-item => (eh/links-to? [:.clj--activity-item__link] "http://objective8.dcentproject.eu/objectives/6/questions/23")
        third-activity-item => (eh/has-attr? [:.clj--activity-item__time] :datetime "2015-08-04T14:49:38.407Z")
        third-activity-item => (eh/text-is? [:.clj--activity-item__time] "2015-08-04 14:49:38")
        third-activity-item => (eh/text-is? [:.clj--activity-item__action__author] "Lala")
        third-activity-item => (eh/has-attr? [:.clj--activity-item__action] :data-l8n "content:activity-type/action-text-objective-question")
        third-activity-item => (eh/text-is? [:.clj--activity-item__title] "QUESTION 6 TITLE")
        third-activity-item => (eh/has-attr? [:.clj--activity-item__id] :hidden "hidden")
        third-activity-item => (eh/text-is? [:.clj--activity-item__id] "3")))

(fact "activity action text varies depending on existence of target"
      (let [page (fv/feed {:context {:activities
                                     [{:type   "Question"
                                       :object {:type        "Objective Question"
                                                :displayName "QUESTION 6 TITLE"
                                                :url         "http://objective8.dcentproject.eu/objectives/6/questions/23"}}
                                      {:type   "Question"
                                       :object {:type        "Objective Question"
                                                :displayName "QUESTION 7 TITLE"
                                                :url         "http://objective8.dcentproject.eu/objectives/7/questions/23"}
                                       :target {:type        "Objective"
                                                :displayName "OBJECTIVE 7 TITLE OBJECTIVE 7 TITLE OBJECTIVE 7 TITLE OBJECTIVE 7 TITLE OBJECTIVE 7 TITLE"
                                                :url         "http://objective8.dcentproject.eu/objectives/7"}}]}})
            [activity-without-target activity-with-target] (html/select page [:.clj--activity-item])]
        activity-with-target => (eh/text-is? [:.clj--activity-item__target] "OBJECTIVE 7 TITLE OBJECTIVE 7 TITLE…")
        activity-with-target => (eh/has-attr? [:.activity-item__action__target html/last-child] :href "http://objective8.dcentproject.eu/objectives/7")
        activity-with-target => (eh/has-attr? [:.clj--activity-item__connector] :data-l8n "content:feed/action-text-connector-about")

        activity-without-target => (eh/text-is? [:.clj--activity-item__target] "")
        activity-without-target => (eh/does-not-exist? [:.activity-item__action__target [html/last-child (html/attr? :href)]])
        activity-without-target => (eh/does-not-exist? [:.clj--activity-item__connector])))

(fact "activity item avatars are given the initial of the actor (the name of the person)"
      (let [page (fv/feed {:context {:activities
                                     [{:activity-src "an-activity-src"
                                       :actor        {:type        "Person"
                                                      :displayName "abby"}}
                                      {:activity-src "an-activity-src"
                                       :actor        {:type        "Person"
                                                      :displayName "Bobby"}}
                                      {:activity-src "an-activity-src"
                                       :actor        {:type        "Person"
                                                      :displayName "2k12carlos"}}]}})
            initials-elements (-> (html/select page [:.clj--avatar__initials]))]
        (html/text (first initials-elements)) => "A"
        (html/text (second initials-elements)) => "B"
        (html/text (nth initials-elements 2 nil)) => "2"))

(fact "activity item avatars are assigned the correct classes so they can be colour-coded by activity source"
      (let [page (fv/feed {:context {:activity-sources
                                     {:an-activity-src      {:index 0}
                                      :another-activity-src {:index 1}}
                                     :activities
                                     [{:activity-src "an-activity-src"}
                                      {:activity-src "another-activity-src"}
                                      {:activity-src "an-activity-src"}]}})
            first-activity-item-class (-> (html/select page [:.clj--activity-item])
                                          first :attrs :class)
            second-activity-item-class (-> (html/select page [:.clj--activity-item])
                                           second :attrs :class)
            third-activity-item-class (-> (html/select page [:.clj--activity-item])
                                          (nth 2 nil) :attrs :class)]
        first-activity-item-class => (contains "activity-src-0")
        first-activity-item-class =not=> (contains "activity-src-1")
        (count (re-seq #"activity-src-" first-activity-item-class)) => 1
        second-activity-item-class => (contains "activity-src-1")
        second-activity-item-class =not=> (contains "activity-src-0")
        (count (re-seq #"activity-src-" second-activity-item-class)) => 1
        third-activity-item-class => (contains "activity-src-0")
        third-activity-item-class =not=> (contains "activity-src-1")
        (count (re-seq #"activity-src-" third-activity-item-class)) => 1))

(facts "about activities"
       (facts "when empty"
              (let [page (fv/feed {:context {:activities []}})]
                (fact "message indicating no retrieved activities"
                      (-> page (html/select [:.clj--empty-activity-item]) first) =not=> nil?)
                (fact "message indicating no retrieved activities links to the customise feed page"
                      page => (eh/links-to? [:.clj--empty-stream__link] (routes/path :show-customise-feed)))
                (fact "message indicating no retrieved activities is translated"
                      (eh/test-translations "feed page - no activity sources message" (constantly page)))))
       (facts "when not empty"
              (let [page (fv/feed {:context {:activities [...something...]}})]
                (fact "message indicating no retrieved activities is not shown"
                      (-> page (html/select [:.clj--empty-activity-item]) first) => nil?))))

(fact "activity warning messages are rendered on the page"
      (let [page (fv/feed {:context {:activities
                                                                  [{:activity-src        "an-objective8-activity-src"
                                                                    (keyword "@context") "http://www.w3.org/ns/activitystreams"
                                                                    :type                "Create"
                                                                    :actor               {:type        "Person"
                                                                                          :displayName "JDog"}
                                                                    :object              {:type        "Objective"
                                                                                          :displayName (str "Lorem ipsum dolor sit amet, consectetur "
                                                                                                            "adipiscing elit. Morbi nunc tortor, eleifend et egestas sit "
                                                                                                            "amet, tincidunt ac augue. Mauris pellentesque sed.")
                                                                                          :url         "http://objective8.dcentproject.eu/objectives/7"}
                                                                    :signed              true}
                                                                   {:activity-src        "an-objective8-activity-src"
                                                                    (keyword "@context") "http://www.w3.org/ns/activitystreams"
                                                                    :type                "Create"
                                                                    :actor               {:type        "Person"
                                                                                          :displayName "HCat"}
                                                                    :object              {:type        "Objective"
                                                                                          :displayName (str "Loremxipsumxdolorxsitxametyxconsecteturx"
                                                                                                            "adipiscingxelitzxMorbixnuncxtortoryxeleifendxetxegestasxsitx"
                                                                                                            "ametyxtinciduntxacxauguezxMaurisxpellentgfdogk")
                                                                                          :url         "http://objective8.dcentproject.eu/objectives/7"}
                                                                    :signed              false}
                                                                   {:activity-src        "an-objective8-activity-src"
                                                                    (keyword "@context") "http://www.w3.org/ns/activitystreams"
                                                                    :type                "Create"
                                                                    :actor               {:type        "Person"
                                                                                          :displayName "QRacoon"}
                                                                    :object              {:type        "Objective"
                                                                                          :displayName (str "Loremxipsumxdolorxsitxametyxconsecteturx"
                                                                                                            "adipiscingxelitzxMorbixnuncxtortoryxeleifendxetxegestasxsitx"
                                                                                                            "ametyxtinciduntxacxauguezxMaurisxpellentgfdogk")
                                                                                          :url         "http://objective8.dcentproject.eu/objectives/7"}
                                                                    :signed              "verification-failed"}]
                                     :active-activity-source-keys [...active-activity-source-key...]}})
            [first-activity-item second-activity-item third-activity-item] (html/select page [:.clj--activity-item])]

        first-activity-item => (eh/text-is? [:.clj--activity-item__title] (str "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Morbi nunc tortor, "
                                                                               "eleifend et egestas sit amet, tincidunt ac augue. Mauris\u2026"))
        second-activity-item => (eh/text-is? [:.clj--activity-item__title] (str "LoremxipsumxdolorxsitxametyxconsecteturxadipiscingxelitzxMorbixnuncxtortoryxeleifendxe"
                                                                                "txegestasxsitxametyxtinciduntxacxauguezxMaurisxpellent\u2026"))
        (facts "about warning messages"
               (fact "no warning sign gets displayed if the activity is signed"
                     first-activity-item =not=> (eh/has-class? [:.clj--activity-item] "clj--activity-item__suspicious"))
               (fact "a corresponding warning sign gets displayed if the activity is unsigned"
                     second-activity-item =not=> (eh/has-class? [:.clj--activity-item__suspicious] "clj--STRIP")
                     second-activity-item => (eh/has-class? [:.clj--activity-item__suspicious] "clj--activity-item__suspicious--untrusted-source"))
               (fact "a corresponding warning sign gets displayed if verification of the activity failed"
                     third-activity-item =not=> (eh/has-class? [:.clj--activity-item__suspicious] "clj--STRIP")
                     third-activity-item => (eh/has-class? [:.clj--activity-item__suspicious] "clj--activity-item__suspicious--unverified-signature")))))
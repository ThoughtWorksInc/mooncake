(ns mooncake.test.view.customise-feed
  (:require [midje.sweet :refer :all]
            [net.cgrand.enlive-html :as html]
            [mooncake.test.test-helpers.enlive :as eh]
            [mooncake.routes :as r]
            [mooncake.view.customise-feed :as cf]))

(fact "create-feed page should render create-feed template"
      (let [page (cf/customise-feed ...request...)]
        page => (eh/has-class? [:body] "func--customise-feed-page")))

(eh/test-translations "customise-feed page" cf/customise-feed)
(eh/test-logo-link cf/customise-feed)

(fact "username is rendered"
      (cf/customise-feed {:session {:username "Dave"}}) => (eh/text-is? [:.clj--username] "Dave"))

(fact "sign-out link is rendered and directs to /sign-out when user is signed in"
      (let [page (cf/customise-feed {:session {:username ...username...}})]
        page => (eh/links-to? [:.clj--sign-out__link] (r/path :sign-out))
        page =not=> (eh/has-class? [:.clj--sign-out__link] "clj--STRIP")))

(fact "sign-out link is not rendered if user is not signed in"
      (let [page (cf/customise-feed {})]
        page => (eh/has-class? [:.clj--sign-out__link] "clj--STRIP")))

(fact "customise-feed link is rendered and directs to /customise-feed when user is signed in"
      (let [page (cf/customise-feed {:session {:username ...username...}})]
        page => (eh/links-to? [:.clj--customise-feed__link] (r/path :show-customise-feed))
        page =not=> (eh/has-class? [:.clj--customise-feed__link] "clj--STRIP")))

(fact "customise-feed link is not rendered if user is not signed in"
      (let [page (cf/customise-feed {})]
        page => (eh/has-class? [:.clj--customise-feed__link] "clj--STRIP")))

(fact "customise feed form action is set correctly"
      (let [page (cf/customise-feed ...request...)]
        page => (every-checker
                  (eh/has-form-method? "post")
                  (eh/has-form-action? (r/path :customise-feed)))))

(facts "available feed sources are displayed with the user's preferences"
       (let [activity-source-preferences [{:id       "activity-src"
                                           :name     "Activity Source"
                                           :url      "some url"
                                           :activity-types [{:id "activity-src-activity-type-1"
                                                             :selected false}
                                                            {:id "activity-src-activity-type-2"
                                                             :selected true}]}
                                          {:id       "another-activity-src"
                                           :name     "Another Source"
                                           :url      "other url"
                                           :activity-types [{:id "another-activity-src-activity-type-1"
                                                             :selected false}]}]
             context {:activity-source-preferences activity-source-preferences}
             page (cf/customise-feed {:context context})]
         (fact "only items generated for activity sources are present"
               (count (html/select page [:.clj--feed-item])) => 2)

         (fact "only provided activity types of activity sources are present"
               (count (html/select page [:.clj--feed-item-child])) => 3)

         (fact "names of activity sources are displayed"
               (let [[first-activity-source-label-name second-activity-source-label-name]
                     (html/select page [:.clj--feed-item__name])]
                 (html/text first-activity-source-label-name) => "Activity Source"
                 (html/text second-activity-source-label-name) => "Another Source"))

         (fact "names of provided activity types of activity sources are displayed"
               (let [[first-activity-type-label-name second-activity-type-label-name third-activity-type-label-name]
                     (html/select page [:.clj--feed-item-child__name])]
                 (html/text first-activity-type-label-name) => "activity-src-activity-type-1"
                 (html/text second-activity-type-label-name) => "activity-src-activity-type-2"
                 (html/text third-activity-type-label-name) => "another-activity-src-activity-type-1"))

         (fact "name attributes for provided activity types of activity sources selection checkboxes are set correctly"
               (let [[first-activity-type-checkbox second-activity-type-checkbox third-activity-type-checkbox]
                     (html/select page [:.clj--feed-item-child__checkbox])]
                 (:attrs first-activity-type-checkbox) => (contains {:name "activity-src||activity-src-activity-type-1"})
                 (:attrs second-activity-type-checkbox) => (contains {:name "activity-src||activity-src-activity-type-2"})
                 (:attrs third-activity-type-checkbox) => (contains {:name "another-activity-src||another-activity-src-activity-type-1"})))

         (fact "'for' attributes of activity types labels match 'id' attributes of activity types inputs"
               (let [[first-activity-type-label second-activity-type-label third-activity-type-label] (html/select page [:.clj--feed-item-child__label])
                     first-label-checkbox  (first (html/select first-activity-type-label [:.clj--feed-item-child__checkbox]))
                     second-label-checkbox (first (html/select second-activity-type-label [:.clj--feed-item-child__checkbox]))
                     third-label-checkbox  (first (html/select third-activity-type-label [:.clj--feed-item-child__checkbox]))]
                 (-> first-activity-type-label :attrs :for) => (-> first-label-checkbox :attrs :id)
                 (-> second-activity-type-label :attrs :for) => (-> second-label-checkbox :attrs :id)
                 (-> third-activity-type-label :attrs :for) => (-> third-label-checkbox :attrs :id)
                 (-> first-label-checkbox :attrs :id) =not=> (-> second-label-checkbox :attrs :id)))

         (fact "selected activity types are checked"
               (let [[first-activity-type-checkbox second-activity-type-checkbox third-activity-type-checkbox]
                     (html/select page [:.clj--feed-item-child__checkbox])]
                 (contains? (:attrs first-activity-type-checkbox) :checked) => falsey
                 (:attrs second-activity-type-checkbox) => (contains {:checked "checked"})
                 (contains? (:attrs third-activity-type-checkbox) :checked) => falsey))))

(facts "available feed sources are displayed if no activity types are available"
       (let [activity-source-preferences [{:id       "activity-src"
                                           :name     "Activity Source"
                                           :url      "some url"
                                           :selected true
                                           :activity-types []}
                                          {:id       "another-activity-src"
                                           :name     "Another Source"
                                           :url      "other url"
                                           :selected false}]
             context {:activity-source-preferences activity-source-preferences}
             page (cf/customise-feed {:context context})]

         (fact "names of activity sources are displayed"
               (let [[first-activity-source-label-name second-activity-source-label-name]
                     (html/select page [:.clj--feed-item__name])]
                 (html/text first-activity-source-label-name) => "Activity Source"
                 (html/text second-activity-source-label-name) => "Another Source"))

         (fact "no acitivity type items are displayed"
               (html/select page [:.clj--feed-item-child]) => empty?)))
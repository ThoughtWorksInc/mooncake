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

(fact "customise feed form action is set correctly"
      (let [page (cf/customise-feed ...request...)]
        page => (every-checker
                  (eh/has-form-method? "post")
                  (eh/has-form-action? (r/path :customise-feed)))))

(facts "available feed sources are displayed with the user's preferences"
       (let [activity-source-preferences [{:id       "activity-src"
                                           :name     "Activity Source"
                                           :url      "some url"
                                           :selected true}
                                          {:id       "another-activity-src"
                                           :name     "Another Source"
                                           :url      "other url"
                                           :selected false}]
             context {:activity-source-preferences activity-source-preferences}
             page (cf/customise-feed {:context context})]
         (fact "only items generated for activity sources are present"
               (count (html/select page [:.clj--feed-item])) => 2)

         (fact "names of activity sources are displayed"
               (let [[first-label-name second-label-name] (html/select page [:.clj--feed-item__name])]
                 (html/text first-label-name) => "Activity Source"
                 (html/text second-label-name) => "Another Source"))

         (fact "name attributes for activity source selection checkboxes are set correctly"
               (let [[first-checkbox second-checkbox] (html/select page [:.clj--feed-item__checkbox])]
                 (:attrs first-checkbox) => (contains {:name "activity-src"})
                 (:attrs second-checkbox) => (contains {:name "another-activity-src"})))

         (fact "'for' attributes of labels match 'id' attributes of inputs"
               (let [[first-label second-label] (html/select page [:.clj--feed-item__label])
                     first-label-checkbox (first (html/select first-label [:.clj--feed-item__checkbox]))
                     second-label-checkbox (first (html/select second-label [:.clj--feed-item__checkbox]))]
                 (-> first-label :attrs :for) => (-> first-label-checkbox :attrs :id)
                 (-> second-label :attrs :for) => (-> second-label-checkbox :attrs :id)
                 (-> first-label-checkbox :attrs :id) =not=> (-> second-label-checkbox :attrs :id)))

         (fact "selected activity sources are checked"
               (let [[first-checkbox second-checkbox] (html/select page [:.clj--feed-item__checkbox])]
                 (:attrs first-checkbox) => (contains {:checked "checked"})
                 (contains? (:attrs second-checkbox) :checked) => falsey))))

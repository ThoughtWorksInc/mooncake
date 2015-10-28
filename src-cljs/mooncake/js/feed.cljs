(ns mooncake.js.feed
  (:require [ajax.core :refer [GET]]
            [dommy.core :as d]
            [hickory.core :as hic]
            [hickory.render :as hic-r]
            [hickory.select :as hic-s]
            [hickory.zip :as hic-z]
            [clojure.zip :as zip]
            [cognitect.transit :as t])
  (:require-macros [dommy.core :as dm]))

(def r (t/reader :json))

(defn handler [response]
  (let [activities (get response "activities")
        feed-items (. (dm/sel1 :.clj--activity-stream) -innerHTML)
        feed-items-as-hic (map hic/as-hickory (hic/parse-fragment feed-items))
        feed-item (first feed-items-as-hic)]
    (doseq [activity activities]
      (let [new-feed-item (d/create-element "li")
            feed-item-html (hic-r/hickory-to-html feed-item)
            old-activity-source-class (re-find #"activity-src-\d+" feed-item-html)]
        (set! (. new-feed-item -innerHTML) feed-item-html)
        (let [author (get-in activity ["actor" "displayName"])
              initial (-> author first str clojure.string/upper-case)
              time (get activity "published")
              title (get activity "limited-title")
              link (get-in activity ["object" "url"])
              action (get-in activity ["object" (keyword "@type")])
              human-readable-time (get activity "formatted-time")
              new-activity-source-class (str "activity-src-" (get activity "activity-src-no"))
              action-text (get activity "action-text")]
          (d/set-text! (dm/sel1 new-feed-item :.clj--avatar__initials) initial)
          (d/set-text! (dm/sel1 new-feed-item :.clj--activity-item__action__author) author)
          (d/set-text! (dm/sel1 new-feed-item :.clj--activity-item__title) title)
          (d/set-text! (dm/sel1 new-feed-item :.clj--activity-item__time) human-readable-time)
          (d/set-attr! (dm/sel1 new-feed-item :.clj--activity-item__time) "datetime" time)
          (d/set-attr! (dm/sel1 new-feed-item :.clj--activity-item__link) "href" link)
          (d/set-text! (dm/sel1 new-feed-item :.clj--activity-item__action) action)
          (d/remove-class! (dm/sel1 new-feed-item :.clj--activity-item) old-activity-source-class)
          (d/add-class! (dm/sel1 new-feed-item :.clj--activity-item) new-activity-source-class)
          (d/remove-attr! (dm/sel1 new-feed-item :.clj--activity-item__action) "data-l8n")
          (d/set-text! (dm/sel1 new-feed-item :.clj--activity-item__action) action-text)
          (d/append! (dm/sel1 :.clj--activity-stream) (.-firstElementChild new-feed-item)))))))

(defn error-handler [response]
  (.log js/console (str "something bad happened: " response)))

(def load-activities-link ".clj--load-activities__link")

(defn load-more-activities [e]
  (let [stream (dm/sel1 :.clj--activity-stream)
        last-activity (.-lastChild stream)
        selector (dm/sel1 last-activity :.clj--activity-item__time)
        timestamp (d/attr selector "datetime")]
    (GET (str "/api/activities?timestamp=" timestamp)
         {:handler       handler
          :error-handler error-handler})))
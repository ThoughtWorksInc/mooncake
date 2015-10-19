(ns mooncake.js.feed
  (:require [ajax.core :refer [GET]]
            [dommy.core :as d]
            [hickory.core :as hic]
            [hickory.render :as hic-r]
            [hickory.select :as hic-s])
  (:require-macros [dommy.core :as dm]))

(defn handler [response]
  (let [activities (hic-s/select (hic-s/class "clj--activity-item") (hic/as-hickory (hic/parse response)))]
    (doseq [activity activities]
      (let [el (d/create-element "li")]
        (set! (. el -innerHTML) (hic-r/hickory-to-html activity))
        (d/append! (dm/sel1 :ul) (.-firstElementChild el))
        (.log js/console (str "Appending " el " to list"))))))

(defn error-handler [response]
  (.log js/console (str "something bad happened: " response)))

(def load-activities-link ".clj--load-activities__link")

(defn load-more-activities [e]
  (.log js/console e)
  (GET "/"
       {:params        {:page-number 2}
        :handler       handler
        :error-handler error-handler}))
(ns mooncake.domain.activity)

(defn activity->activity-src [activity]
  (:activity-src activity))

(defn activity->published [activity]
  (:published activity))

(defn activity->insert-id [activity]
  (str (:relInsertTime activity)))

(defn activity->actor-display-name [activity]
  (get-in activity [:actor :name]))

(defn activity->object-url [activity]
  (get-in activity [:object :url]))

(defn activity->object-type [activity]
  (get-in activity [:object :type]))

(defn activity->object-display-name [activity]
  (get-in activity [:object :name]))

(defn activity->type [activity]
  (:type activity))

(defn activity->default-action-text [activity]
  (str "- " (activity->object-type activity) " - " (activity->type activity)))

(defn activity->signed [activity]
  (:signed activity))

(defn activity->target [activity]
  (get-in activity [:target :name]))

(defn activity->target-url [activity]
  (get-in activity [:target :url]))
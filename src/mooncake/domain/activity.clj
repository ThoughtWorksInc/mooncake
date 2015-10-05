(ns mooncake.domain.activity)

(defn activity->activity-src [activity]
  (:activity-src activity))

(defn activity->published [activity]
  (:published activity))

(defn activity->actor-display-name [activity]
  (get-in activity [:actor :displayName]))

(defn activity->object-url [activity]
  (get-in activity [:object :url]))

(defn activity->object-type [activity]
  (get-in activity [:object (keyword "@type")]))

(defn activity->object-display-name [activity]
  (get-in activity [:object :displayName]))

(defn activity->type [activity]
  ((keyword "@type") activity))

(defn activity->action-text-key [activity]
  (let [activity-object-type (activity->object-type activity)
        activity-type (activity->type activity)]
    (if (= activity-type "Question")
      :question
      (if (= activity-object-type "Objective")
        :objective
        :default))))

(defn activity->default-action-text [activity]
  (str "- " (activity->object-type activity) " - " (activity->type activity)))
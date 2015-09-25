(ns mooncake.domain.activity)

(defn activity->activity-src [activity]
  (or (get activity :activity-src)
      (get activity "activity-src")))

(defn activity->published [activity]
  (or (get activity :published)
      (get activity "published")))

(defn activity->actor-display-name [activity]
  (get-in activity [:actor :displayName]))

(defn activity->object-url [activity]
  (get-in activity [:object :url]))

(defn activity->object-type [activity]
  (get-in activity [:object (keyword "@type")]))

(defn activity->object-display-name [activity]
  (get-in activity [:object :displayName]))

(defn activity->type [activity]
  (get activity (keyword "@type")))

(defn activity->action-text-key [activity]
  (let [activity-object-type (get-in activity [:object (keyword "@type")])
        activity-type (get activity (keyword "@type"))]
    (if (= activity-type "Question")
      :question
      (if (= activity-object-type "Objective")
        :objective
        :default))))

(defn activity->default-action-text [activity]
  (str "- " (activity->object-type activity) " - " (activity->type activity)))
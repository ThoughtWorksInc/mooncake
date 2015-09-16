(ns mooncake.domain.activity)

(defn activity->activity-src [activity]
  (get activity "activity-src"))

(defn activity->published [activity]
  (get activity "published"))

(defn activity->actor-display-name [activity]
  (get-in activity ["actor" "displayName"]))

(defn activity->object-url [activity]
  (get-in activity ["object" "url"]))

(defn activity->object-type [activity]
  (get-in activity ["object" "@type"]))

(defn activity->object-display-name [activity]
  (get-in activity ["object" "displayName"]))

(defn activity->type [activity]
  (get activity "@type"))

(defn activity->action-text-key [activity]
  (let [activity-object-type (get-in activity ["object" "@type"])
        activity-type (get activity "@type")]
    (if (= activity-type "Question")
      :question
      (if (= activity-object-type "Objective")
        :objective
        :default))))

(defn activity->default-action-text [activity]
  (str "- " (activity->object-type activity) " - " (activity->type activity)))
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

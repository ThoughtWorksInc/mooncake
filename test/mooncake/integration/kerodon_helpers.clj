(ns mooncake.integration.kerodon-helpers
  (:require [midje.sweet :refer :all]
            [net.cgrand.enlive-html :as html]
            [kerodon.core :as k]
            [clojure.string :as string]))

(defn page-title [state]
  (-> state :enlive (html/select [:title]) first html/text))

(defn page-title-is [state title]
  (fact {:midje/name "Checking page title:"}
        (page-title state) => title)
  state)

(defn page-uri-is [state uri]
  (fact {:midje/name "Checking page uri:"}
        (-> state :request :uri) => uri)
  state)

(defn page-uri-contains [state uri]
  (fact {:midje/name (str "Checking if page uri contains " uri)}
        (-> state :request :uri) => (contains uri))
  state)

(defn response-status-is [state status]
  (fact {:midje/name (str "Checking response status is " status)}
        (-> state :response :status) => status)
  state)

(defn response-body-contains [state content]
  (fact {:midje/name (str "Check body contains " content)}
        (-> state :response :body) => (contains content))
  state)

(defn check-and-follow [state kerodon-selector]
  (fact {:midje/name (format "Attempting to follow link with selector: %s" kerodon-selector)}
        (let [enlive-selector [kerodon-selector]]
          (-> state :enlive (html/select enlive-selector) first :attrs) => (contains {:href anything})))
  (try (k/follow state kerodon-selector)
       (catch Exception state)))

(defn check-and-follow-redirect
  ([state description]
   "Possibly a double redirect"
   (fact {:midje/name (format "Attempting to follow redirect - %s" description)}
         (-> state :response :status) => 302)
   (try (k/follow-redirect state)
        (catch Exception state)))
  ([state]
   (check-and-follow-redirect state "")))

(defn check-and-fill-in [state kerodon-selector value]
  (fact {:midje/name (format "Attempting to fill in input with selector: %s" kerodon-selector)}
        (let [enlive-selector [kerodon-selector]]
          (-> state :enlive (html/select enlive-selector) first :tag) => :input))
  (try (k/fill-in state kerodon-selector value)
       (catch Exception state)))

(defn check-and-press [state kerodon-selector]
  (fact {:midje/name (format "Attempting to press submit button with selector: %s" kerodon-selector)}
        (let [enlive-selector [kerodon-selector]]
          (-> state :enlive (html/select enlive-selector) first :attrs :type) => "submit"))
  (try (k/press state kerodon-selector)
       (catch Exception state)))

(defn selector-exists [state kerodon-selector]
  (fact {:midje/name (str "Check element exists with " kerodon-selector)}
        (-> state :enlive (html/select [kerodon-selector])) =not=> empty?)
  state)

(defn selector-not-present [state kerodon-selector]
  (fact {:midje/name (str "Check element does not exist with " kerodon-selector)}
        (-> state :enlive (html/select [kerodon-selector])) => empty?)
  state)

(defn selector-includes-content [state kerodon-selector content]
  (fact {:midje/name "Check if element contains string"}
        (-> state :enlive (html/select [kerodon-selector]) first html/text) => (contains content))
  state)

(defn selector-has-attribute-with-content
  ([state kerodon-selector attr content]
    (selector-has-attribute-with-content state kerodon-selector first attr content))
  ([state kerodon-selector position-fn attr content]
   (fact {:midje/name "Check if element contains attribute with string"}
         (-> state :enlive (html/select [kerodon-selector]) position-fn :attrs attr) => content)
   state))

(defn selector-does-not-include-content [state kerodon-selector content]
  (fact {:midje/name "Check if element does not contain string"}
        (-> state :enlive (html/select [kerodon-selector]) first html/text) =not=> (contains content))
  state)

(defn location-contains [state path]
  (fact {:midje/name "Checking location in header:"}
        (-> state :response (get-in [:headers "Location"])) => (contains path))
  state)

(ns mooncake.integration.kerodon-helpers
  (:require [midje.sweet :refer :all]
            [cheshire.core :as json]
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


(defn check-and-follow-redirect
  ([state description]
   "Possibly a double redirect"
   (fact {:midje/name (format "Attempting to follow redirect - %s" description)}
         (-> state :response :status) => 302)
   (try (k/follow-redirect state)
        (catch Exception state)))
  ([state]
   (check-and-follow-redirect state "")))


(defn selector-exists [state selector]
  (fact {:midje/name (str "Check element exists with " selector)}
        (-> state :enlive (html/select selector)) =not=> empty?)
  state)

(defn selector-not-present [state selector]
  (fact {:midje/name (str "Check element does not exist with " selector)}
        (-> state :enlive (html/select selector)) => empty?)
  state)

(defn selector-includes-content [state selector content]
  (fact {:midje/name "Check if element contains string"}
        (-> state :enlive (html/select selector) first html/text) => (contains content))
  state)

(defn selector-has-attribute-with-content [state selector attr content]
  (fact {:midje/name "Check if element contains attribute with string"}
        (-> state :enlive (html/select selector) first :attrs attr) => content)
  state)

(defn selector-does-not-include-content [state selector content]
  (fact {:midje/name "Check if element does not contain string"}
        (-> state :enlive (html/select selector) first html/text) =not=> (contains content))
  state)

(defn location-contains [state path]
  (fact {:midje/name "Checking location in header:"}
        (-> state :response (get-in [:headers "Location"])) => (contains path))
  state)

(defn response-has-access-token [state]
  (fact {:midje/name "Checking if response has access bearer token"}
        (let [response-body (-> state
                                :response
                                :body
                                (json/parse-string keyword))]
          (:access_token response-body) => (just #"[A-Z0-9]{32}")
          (:token_type response-body) => "bearer"))
  state)

(defn response-has-user-email [state email]
  (fact {:midje/name "Checking if response has user email"}
        (let [response-body (-> state
                                :response
                                :body
                                (json/parse-string keyword))]
          (:user-email response-body) => email))
  state)

(defn response-has-id [state]
  (fact {:midje/name "Checking if response has user id"}
        (let [response-body (-> state
                                :response
                                :body
                                (json/parse-string keyword))]
          (:user-id response-body) =not=> string/blank?))
  state)

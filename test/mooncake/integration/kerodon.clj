(ns mooncake.integration.kerodon
  (:require [midje.sweet :refer :all]
            [kerodon.core :as k]
            [mooncake.handler :as h]
            [mooncake.integration.kerodon-helpers :as kh]
            [mooncake.integration.kerodon-selectors :as ks]))

(defn print-enlive [state]
  (prn (-> state :enlive))
  state)

(defn print-request [state]
  (prn (-> state :request))
  state)

(defn print-state [state]
  (prn state)
  state)


(future-facts "Go to /"
       (-> (k/session h/app)
           (k/visit "/")
           (kh/page-uri-is "/")
           (kh/response-status-is 200)
           (kh/selector-exists [ks/index-page-body])))

(facts "Going to an unknown uri renders the 404 page"
       (-> (k/session h/app)
           (k/visit "/not-a-valid-uri")
           (kh/page-uri-is "/not-a-valid-uri")
           (kh/response-status-is 404)
           (kh/selector-exists [ks/error-404-page-body])))

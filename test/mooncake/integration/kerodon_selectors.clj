(ns mooncake.integration.kerodon-selectors
  (:require
            [net.cgrand.enlive-html :as html]
            ))

(def header-sign-out-link :.func--sign-out__link)
(def index-page-body :.func--index-page)
(def index-page-activity-item-title :.func--activity-item__title)
(def index-page-activity-item-action :.func--activity-item__action)
(def index-page-activity-item-link :.func--activity-item__link)
(def sign-in-page-body :.func--sign-in-page)
(def sign-in-page-sign-in-with-d-cent-link :.func--sign-in-with-d-cent)
(def error-500-page-body :.func--error-500-page)
(def error-404-page-body :.func--error-404-page)
(def create-account-page-username-input :.func--username__input)
(def create-account-page-submit-button :.func--create-account__button)
(def customise-feed-page-body :.func--customise-feed-page)
(def customise-feed-page-feed-item-list :.func--customise-feed__list)
(def customise-feed-page-feed-item-checkbox :.func--feed-item__checkbox)

(ns mooncake.js.client-translation
  (:require-macros [taoensso.tower :as tower-macros])
  (:require [taoensso.tower :as tower]))

(def ^:private tconfig
  {:fallback-locale :en
   :compiled-dictionary (tower-macros/dict-compile "client-translations.clj")})

(def t (tower/make-t tconfig))

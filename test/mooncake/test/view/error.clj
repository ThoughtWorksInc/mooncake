(ns mooncake.test.view.error
  (:require [midje.sweet :refer :all]
            [net.cgrand.enlive-html :as html]
            [mooncake.view.error :as e]))

(fact "modify error translation keys updates the data-l8n tags of the correct elements"
      (let [modified-error-enlive-map (e/modify-error-translation-keys (e/internal-server-error) "oops-error-page")]
        (-> (html/select modified-error-enlive-map [:title])
            first :attrs :data-l8n) => "content:oops-error-page/title"
        (-> (html/select modified-error-enlive-map [:.clj--error-page-header])
            first :attrs :data-l8n) => "content:oops-error-page/page-header"
        (-> (html/select modified-error-enlive-map [:.clj--error-page-intro])
            first :attrs :data-l8n) => "content:oops-error-page/page-intro"
        (-> (html/select modified-error-enlive-map [:.clj--error-page-content])
            first :attrs :data-l8n) => "html:oops-error-page/page-content"))

(fact "internal-server-error should return html with the correct body class"
      (let [page (e/internal-server-error)]
        (-> (html/select page [:body]) first :attrs :class) => "func--error-500-page"))

(fact "not-found-error should return html with the correct body class"
      (let [page (e/not-found-error)]
        (-> (html/select page [:body]) first :attrs :class) => "func--error-404-page"))

(fact "forbidden-error should return html with the correct body class"
      (let [page (e/forbidden-error)]
        (-> (html/select page [:body]) first :attrs :class) => "func--error-forbidden-page"))

(ns mooncake.test.view.error
  (:require [midje.sweet :refer :all]
            [net.cgrand.enlive-html :as html]
            [mooncake.test.test-helpers :as th]
            [mooncake.view.error :as e]))

(fact "modify error translation keys updates the data-l8n tags of the correct elements"
      (let [enlive-m (e/modify-error-translation-keys (e/internal-server-error) "oops-error-page")]
        enlive-m => (th/has-attr? [:title] :data-l8n "content:oops-error-page/title")
        enlive-m => (th/has-attr? [:.clj--error-page-header] :data-l8n "content:oops-error-page/page-header")
        enlive-m => (th/has-attr? [:.clj--error-page-intro] :data-l8n "content:oops-error-page/page-intro")
        enlive-m => (th/has-attr? [:.clj--error-page-content] :data-l8n "html:oops-error-page/page-content")))

(fact "internal-server-error should return html with the correct body class"
      (let [page (e/internal-server-error)]
        page => (th/has-class? [:body] "func--error-500-page")))

(fact "not-found-error should return html with the correct body class"
      (let [page (e/not-found-error)]
        page => (th/has-class? [:body] "func--error-404-page")))

(fact "forbidden-error should return html with the correct body class"
      (let [page (e/forbidden-error)]
        page => (th/has-class? [:body] "func--error-forbidden-page")))

(th/test-translations "Internal server error page" (constantly (e/internal-server-error)))
(th/test-translations "Not found error page" (constantly (e/not-found-error)))
(th/test-translations "Forbidden error page" (constantly (e/forbidden-error)))

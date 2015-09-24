(ns mooncake.test.view.error
  (:require [midje.sweet :refer :all]
            [mooncake.test.test-helpers.enlive :as eh]
            [mooncake.view.error :as e]))

(fact "modify error translation keys updates the data-l8n tags of the correct elements"
      (let [enlive-m (e/modify-error-translation-keys (e/internal-server-error ..request...) "oops-error")]
        enlive-m => (eh/has-attr? [:body] :class "func--oops-error-page")
        enlive-m => (eh/has-attr? [:title] :data-l8n "content:oops-error/title")
        enlive-m => (eh/has-attr? [:.clj--error-page-header] :data-l8n "content:oops-error/page-header")
        enlive-m => (eh/has-attr? [:.clj--error-page-intro] :data-l8n "content:oops-error/page-intro")))

(fact "internal-server-error should return html with the correct body class"
      (let [page (e/internal-server-error ...request...)]
        page => (eh/has-class? [:body] "func--error-500-page")))

(fact "not-found-error should return html with the correct body class"
      (let [page (e/not-found-error ...request...)]
        page => (eh/has-class? [:body] "func--error-404-page")))

(fact "forbidden-error should return html with the correct body class"
      (let [page (e/forbidden-error ...request...)]
        page => (eh/has-class? [:body] "func--error-forbidden-page")))

(eh/test-translations "Internal server error page" (constantly (e/internal-server-error ...request...)))
(eh/test-translations "Not found error page" (constantly (e/not-found-error ...request...)))
(eh/test-translations "Forbidden error page" (constantly (e/forbidden-error ...request...)))

(eh/test-logo-link e/internal-server-error)
(eh/test-logo-link e/not-found-error)
(eh/test-logo-link e/forbidden-error)

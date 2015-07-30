(ns oauth-toy.acceptance
  (:require [oauth-toy.test-common :refer :all]
            [clj-http.client :as http]
            [environ.core :refer [env]]
            [midje.sweet :refer :all]))

(fact-group
  :acceptance

  (fact "Ping resource returns 200 HTTP response"
        (let [response (http/get (url+ "/ping") {:throw-exceptions false})]
          response => (contains {:status 200})))

  (fact "Healthcheck resource returns 200 HTTP response"
        (let [response (http/get (url+ "/healthcheck") {:throw-exceptions false})
              body (json-body response)]
          response => (contains {:status 200})
          body => (contains {:name    "oauth-toy"
                             :success true
                             :version truthy})))

  (fact "Request to a users resource returns a o-auth redirect"
        (let [{:keys [headers status]} (http/get (url+ "/user/age") {:follow-redirects false})]
          status => 302
          headers => (contains {"Location" "https://accounts.google.com/o/oauth2/auth?redirect_uri=https%3A%2F%2Fdevelopers.google.com%2Foauthplayground&response_type=code&client_id=407408718192.apps.googleusercontent.com&scope=https%3A%2F%2Fwww.googleapis.com%2Fauth%2Fcalendar.readonly&approval_prompt=force&access_type=offline"})))
  )

(ns oauth-toy.acceptance
  (:require [oauth-toy.test-common :refer :all]
            [clj-http.client :as http]
            [clj-http.util :refer [url-decode]]
            [environ.core :refer [env]]
            [midje.sweet :refer :all]))

(defn url->map
  [url]
  (when url
    (let [url-split (clojure.string/split url #"\?")
          url (first url-split)
          params (clojure.string/split (second url-split) #"&")]
      (->> params
           (map (fn [param] (vec (clojure.string/split param #"="))))
           (map (fn [[k v]] [(keyword (url-decode k)) (url-decode v)]))
           (into {})
           (merge {:url url})))))

(defn valid-client-id
  [client-id]
  (not (nil? client-id)))

(defn valid-authorization-code
  [authorization-code]
  (not (nil? authorization-code)))

(defn get-age
  ([]
   (get-age {}))
  ([m]
   (http/get (url+ "/user/age") (merge {:throw-exceptions false
                                        :follow-redirects false} m))))
(defn get-name
  ([]
   (get-name {}))
  ([m]
   (http/get (url+ "/user/name") (merge {:throw-exceptions false
                                         :follow-redirects false} m))))

(defn follow-redirect
  [{headers :headers}]
  (http/get (get headers "Location") {:throw-exceptions false
                                      :follow-redirects false}))

(fact-group
  :acceptance

  (facts "age"

         (fact "Request to a users resource returns a o-auth redirect"
               (let [{:keys [headers status]} (get-age)
                     location (url->map (get headers "Location"))]
                 status => 302
                 location => (contains {:url          "http://localhost:8080/o/oauth2/auth"
                                        :redirect_uri "http://localhost:8080/user/age"
                                        :access_type  "offline"
                                        :client_id    valid-client-id
                                        :scope        "age"})))

         (fact "After signin return Authorization code"
               ;; follow redirect, this should for our example return a valid authorized token in the next redirect
               (let [{:keys [status headers]} (-> (get-age)
                                                  follow-redirect)
                     location (url->map (get headers "Location"))]
                 status => 302
                 location => (contains {:url                "http://localhost:8080/user/age"
                                        :authorization-code valid-authorization-code})))

         (fact "Get access to secure resourse, will get authorization token and follow redirects to access the resource"
               (get-age {:follow-redirects true}) => (contains {:status 200}))

         (fact "return forbidden on invalid authorization code"
               (let [{:keys [url]} (-> (get-age)
                                       follow-redirect
                                       (get-in [:headers "Location"])
                                       url->map)]
                 (http/get url {:throw-exceptions false
                                :follow-redirects false
                                :query-params     {:authorization-code "invalid_code"}}) => (contains {:status 403})))

         )

  (facts "name"

         (fact "Request to a users resource returns a o-auth redirect"
               (let [{:keys [headers status]} (get-name)
                     location (url->map (get headers "Location"))]
                 status => 302
                 location => (contains {:url          "http://localhost:8080/o/oauth2/auth"
                                        :redirect_uri "http://localhost:8080/user/name"
                                        :access_type  "offline"
                                        :client_id    valid-client-id
                                        :scope        "name"})))

         (fact "After signin return Authorization code"
               ;; follow redirect, this should for our example return a valid authorized token in the next redirect
               (let [{:keys [status headers]} (-> (get-name)
                                                  follow-redirect)
                     location (url->map (get headers "Location"))]
                 status => 302
                 location => (contains {:url                "http://localhost:8080/user/name"
                                        :authorization-code valid-authorization-code})))

         (fact "Get access to secure resourse, will get authorization token and follow redirects to access the resource"
               (get-name {:follow-redirects true}) => (contains {:status 200}))

         (fact "return forbidden on invalid authorization code"
               (let [{:keys [url]} (-> (get-name)
                                       follow-redirect
                                       (get-in [:headers "Location"])
                                       url->map)]
                 (http/get url {:throw-exceptions false
                                :follow-redirects false
                                :query-params     {:authorization-code "invalid_code"}}) => (contains {:status 403})))

         )

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


  (fact "return forbidden when using authorization-code from another resource"
        (let [age-authorization-code (-> (get-age)
                                         (follow-redirect)
                                         (get-in [:headers "Location"])
                                         url->map
                                         :authorization-code)
              {:keys [status]} (http/get (url+ "/user/name") {:throw-exceptions false
                                                              :follow-redirects true
                                                              :query-params     {:authorization-code age-authorization-code}})]
          status => 403))

  (fact "Can request for multiple resources return token, given no redirect_uri"
        (let [{:keys [status body]} (http/get (url+ "/o/oauth2/auth") {:query-params {:scope "name|age"}
                                                                       :throw-exceptions false
                                                                       :as :json})]
          status => 200
          body => (contains {:authorization-code valid-authorization-code})))


  (future-fact "return forbidden for expired authorization-code"
               ;; set date directly via calling oauth resource directly
               )

  (future-fact "Get access to resource if the authorization scope contains the resource"
               ;; call auth directly with scope to create authorization scope
               )

  (future-fact "refresh token - not sure yet will need to have a think about how this works")
  )

(ns oauth-toy.oauth
  (:import (java.util UUID)))


(def valid-codes (atom {}))

(defn generate-new-authorisation-code
  [scope]
  (let [code (str (UUID/randomUUID))]
    (reset! valid-codes (merge @valid-codes {code (-> scope
                                                      (clojure.string/split #"\|")
                                                      set)}))
    code))

(defn is-authorisation-code-valid?
  [code scope]
  (contains? (get @valid-codes code) scope))

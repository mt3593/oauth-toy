(ns oauth-toy.oauth
  (:import (java.util UUID)))


(def valid-codes (atom #{}))

(defn generate-new-authorisation-code
  []
  (let [code (str (UUID/randomUUID))]
    (reset! valid-codes (conj @valid-codes code))
    code))

(defn is-authorisation-code-valid?
  [code]
  (contains? @valid-codes code))
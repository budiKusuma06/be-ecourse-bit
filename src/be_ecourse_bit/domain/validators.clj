(ns be-ecourse-bit.domain.validators
  (:require [malli.core :as malli]))

(def course-schema
  [:map
   [:title [:string {:min 3}]]
   [:description [:string {:min 10}]]
   [:instructor [:string {:min 2}]]
   [:price [:double {:min 0}]]])

(def user-registration-schema
  [:map
   [:username [:string {:min 3 :max 50}]]
   [:email [:string {:min 5}]]
   [:password [:string {:min 6}]]
   [:role {:optional true} [:enum "admin" "instructor" "student"]]])

(def login-schema
  [:map
   [:username [:string]]
   [:password [:string]]])

(defn validate [schema data]
  (malli/explain schema data))
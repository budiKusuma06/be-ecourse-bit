(ns be-ecourse-bit.domain.validators
  (:require [malli.core :as malli]))

(def course-schema
  [:map
   [:title [:string {:min 3}]]
   [:description [:string {:min 10}]]
   [:instructor [:string {:min 2}]]
   [:price [:double {:min 0}]]])

;; Student registration dengan email/password
(def student-registration-schema
  [:map
   [:email [:string {:min 5}]]
   [:password [:string {:min 6}]]
   [:full_name [:string {:min 2}]]
   [:phone {:optional true} [:string]]])

;; Student registration via Google OAuth
(def student-oauth-registration-schema
  [:map
   [:email [:string {:min 5}]]
   [:google_id [:string {:min 1}]]
   [:full_name [:string {:min 2}]]
   [:avatar_url {:optional true} [:string]]])

;; Admin creation (hanya super admin yang bisa)
(def admin-creation-schema
  [:map
   [:email [:string {:min 5}]]
   [:password [:string {:min 6}]]
   [:full_name [:string {:min 2}]]
   [:department {:optional true} [:string]]
   [:position {:optional true} [:string]]
   [:permission_ids {:optional true} [:vector :int]]])

;; Login dengan email/password
(def login-email-schema
  [:map
   [:email [:string]]
   [:password [:string]]])

;; Login via Google OAuth
(def login-google-schema
  [:map
   [:google_id [:string]]])

;; Update student profile
(def student-profile-update-schema
  [:map
   [:full_name {:optional true} [:string {:min 2}]]
   [:phone {:optional true} [:string]]
   [:date_of_birth {:optional true} [:string]]
   [:bio {:optional true} [:string]]
   [:avatar_url {:optional true} [:string]]])

;; Update admin profile
(def admin-profile-update-schema
  [:map
   [:full_name {:optional true} [:string {:min 2}]]
   [:department {:optional true} [:string]]
   [:position {:optional true} [:string]]
   [:phone {:optional true} [:string]]
   [:avatar_url {:optional true} [:string]]])

;; Update admin permissions
(def admin-permissions-update-schema
  [:map
   [:permission_ids [:vector :int]]])

(defn validate [schema data]
  (malli/explain schema data))
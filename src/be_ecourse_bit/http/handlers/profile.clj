(ns be-ecourse-bit.http.handlers.profile
  (:require [be-ecourse-bit.domain.repositories.profile :as profile-repo]
            [be-ecourse-bit.domain.validators :as v]
            [be-ecourse-bit.http.middleware :as mw]
            [malli.error :as me]))

;; ============================================
;; Student Profile
;; ============================================

(defn get-student-profile [profile-repository]
  (fn [request]
    (let [session-user (mw/get-session-user request)]
      (if (= "student" (:role session-user))
        (if-let [profile (profile-repo/get-student-profile profile-repository (:id session-user))]
          {:status 200 :body profile}
          {:status 404 :body {:error "Profile not found"}})
        {:status 403 :body {:error "Not a student"}}))))

(defn update-student-profile [profile-repository]
  (fn [request]
    (let [session-user (mw/get-session-user request)
          input (:body-params request)
          error (v/validate v/student-profile-update-schema input)]
      (if error
        {:status 400 :body {:error (me/humanize error)}}
        (if (= "student" (:role session-user))
          (do
            (profile-repo/update-student-profile! profile-repository (:id session-user) input)
            {:status 200 :body {:message "Profile updated successfully"}})
          {:status 403 :body {:error "Not a student"}})))))

;; ============================================
;; Admin Profile
;; ============================================

(defn get-admin-profile [profile-repository]
  (fn [request]
    (let [session-user (mw/get-session-user request)]
      (if (= "admin" (:role session-user))
        (if-let [profile (profile-repo/get-admin-profile profile-repository (:id session-user))]
          {:status 200 :body profile}
          {:status 404 :body {:error "Profile not found"}})
        {:status 403 :body {:error "Not an admin"}}))))

(defn update-admin-profile [profile-repository]
  (fn [request]
    (let [session-user (mw/get-session-user request)
          input (:body-params request)
          error (v/validate v/admin-profile-update-schema input)]
      (if error
        {:status 400 :body {:error (me/humanize error)}}
        (if (= "admin" (:role session-user))
          (do
            (profile-repo/update-admin-profile! profile-repository (:id session-user) input)
            {:status 200 :body {:message "Profile updated successfully"}})
          {:status 403 :body {:error "Not an admin"}})))))
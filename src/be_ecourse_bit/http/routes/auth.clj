(ns be-ecourse-bit.http.routes.auth
  (:require [be-ecourse-bit.http.handlers.auth :as handlers]
            [be-ecourse-bit.http.middleware :as mw]))

(defn routes [services]
  (let [{:keys [auth-service user-repository]} services]
    [;; ============================================
     ;; Student Registration
     ;; ============================================
     ["/auth/register/student"
      {:post {:handler (handlers/register-student auth-service)}}]

     ["/auth/register/student/oauth"
      {:post {:handler (handlers/register-student-oauth auth-service)}}]

     ;; ============================================
     ;; Login (Unified for Student & Admin)
     ;; ============================================
     ["/auth/login"
      {:post {:handler (handlers/login-email auth-service)}}]

     ["/auth/login/google"
      {:post {:handler (handlers/login-google auth-service)}}]

     ;; ============================================
     ;; Session Management
     ;; ============================================
     ["/auth/logout"
      {:post {:handler (handlers/logout)}}]

     ["/auth/status"
      {:get {:handler (handlers/session-status)}}]

     ;; ============================================
     ;; Profile
     ;; ============================================
     ["/auth/profile"
      {:get {:handler (mw/wrap-auth (handlers/profile user-repository))}}]

     ;; ============================================
     ;; Admin Creation (Super Admin Only)
     ;; ============================================
     ["/auth/admin/create"
      {:post {:handler (mw/wrap-super-admin (handlers/create-admin auth-service))}}]]))
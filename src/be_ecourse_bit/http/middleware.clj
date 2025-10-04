(ns be-ecourse-bit.http.middleware
  (:require [ring.middleware.session :as session]
            [ring.middleware.cookies :as cookies]))

(defn get-session-user [request]
  (get-in request [:session :user]))

(defn wrap-session [handler session-store]
  (-> handler
      (session/wrap-session
       {:store session-store
        :cookie-name "ecourse-session"
        :cookie-attrs {:max-age (* 24 60 60)
                       :http-only true
                       :secure false
                       :same-site :lax}})
      cookies/wrap-cookies))

;; ============================================
;; Authentication Middleware
;; ============================================

(defn wrap-auth
  "Require authentication. Optional: check role or permissions"
  ([handler] (wrap-auth handler {}))
  ([handler opts]
   (fn [request]
     (if-let [session-user (get-session-user request)]
       (handler (assoc request :user session-user))
       {:status 401
        :body {:error "Please login to access this resource"}}))))

;; ============================================
;; Role-based Authorization
;; ============================================

(defn wrap-role
  "Require specific role(s)"
  [handler required-roles]
  (fn [request]
    (if-let [session-user (get-session-user request)]
      (let [user-role (:role session-user)]
        (if (contains? (set required-roles) user-role)
          (handler (assoc request :user session-user))
          {:status 403
           :body {:error "Insufficient permissions - role not allowed"}}))
      {:status 401
       :body {:error "Please login to access this resource"}})))

;; ============================================
;; Permission-based Authorization (Admin only)
;; ============================================

(defn wrap-permission
  "Require specific permission(s) - for admin only"
  [handler required-permissions]
  (fn [request]
    (if-let [session-user (get-session-user request)]
      (if (not= "admin" (:role session-user))
        {:status 403
         :body {:error "Admin access required"}}
        (let [user-permissions (:permissions session-user)
              is-super (:is_super_admin session-user)]
          (if (or is-super
                  (= :all user-permissions)
                  (some (set user-permissions) required-permissions))
            (handler (assoc request :user session-user))
            {:status 403
             :body {:error "Insufficient permissions - missing required permission"}})))
      {:status 401
       :body {:error "Please login to access this resource"}})))

;; ============================================
;; Super Admin Only
;; ============================================

(defn wrap-super-admin
  "Require super admin access"
  [handler]
  (fn [request]
    (if-let [session-user (get-session-user request)]
      (if (and (= "admin" (:role session-user))
               (:is_super_admin session-user))
        (handler (assoc request :user session-user))
        {:status 403
         :body {:error "Super admin access required"}})
      {:status 401
       :body {:error "Please login to access this resource"}})))
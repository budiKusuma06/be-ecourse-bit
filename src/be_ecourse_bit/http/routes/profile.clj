(ns be-ecourse-bit.http.routes.profile
  (:require [be-ecourse-bit.http.handlers.profile :as handlers]
            [be-ecourse-bit.http.middleware :as mw]))

(defn routes [services]
  (let [{:keys [profile-repository]} services]
    [;; ============================================
     ;; Student Profile
     ;; ============================================
     ["/profile/student"
      {:get {:handler (mw/wrap-auth (handlers/get-student-profile profile-repository))}
       :put {:handler (mw/wrap-auth (handlers/update-student-profile profile-repository))}}]

     ;; ============================================
     ;; Admin Profile
     ;; ============================================
     ["/profile/admin"
      {:get {:handler (mw/wrap-auth (handlers/get-admin-profile profile-repository))}
       :put {:handler (mw/wrap-auth (handlers/update-admin-profile profile-repository))}}]]))
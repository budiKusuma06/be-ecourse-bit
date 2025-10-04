(ns be-ecourse-bit.http.routes.admin
  (:require [be-ecourse-bit.http.handlers.admin :as handlers]
            [be-ecourse-bit.http.middleware :as mw]))

(defn routes [services]
  (let [{:keys [admin-service]} services]
    [;; ============================================
     ;; Admin Management (Super Admin Only)
     ;; ============================================
     ["/admin/admins"
      {:get {:handler (mw/wrap-super-admin (handlers/list-admins admin-service))}}]

     ["/admin/admins/:id"
      {:get {:handler (mw/wrap-super-admin (handlers/get-admin admin-service))}}]

     ["/admin/admins/:id/permissions"
      {:get {:handler (mw/wrap-role (handlers/get-admin-permissions admin-service) ["admin"])}
       :put {:handler (mw/wrap-super-admin (handlers/update-admin-permissions admin-service))}}]

     ["/admin/admins/:id/deactivate"
      {:post {:handler (mw/wrap-super-admin (handlers/deactivate-admin admin-service))}}]

     ;; ============================================
     ;; Permissions (Admin can view)
     ;; ============================================
     ["/admin/permissions"
      {:get {:handler (mw/wrap-role (handlers/list-permissions admin-service) ["admin"])}}]]))
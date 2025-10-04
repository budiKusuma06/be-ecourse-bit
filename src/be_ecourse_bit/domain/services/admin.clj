(ns be-ecourse-bit.domain.services.admin
  (:require [be-ecourse-bit.domain.repositories.user :as user-repo]
            [be-ecourse-bit.domain.repositories.profile :as profile-repo]
            [be-ecourse-bit.domain.repositories.permission :as perm-repo]))

(defprotocol AdminService
  (list-all-admins [this])
  (get-admin-detail [this admin-id])
  (update-admin-permissions [this admin-id permission-ids updated-by-id])
  (deactivate-admin [this admin-id updated-by-id])
  (list-all-permissions [this])
  (get-admin-permissions [this admin-id]))

(defrecord DefaultAdminService [user-repository profile-repository permission-repository]
  AdminService
  (list-all-admins [_]
    (let [admins (filter #(= "admin" (:role %))
                         (user-repo/find-all user-repository))]
      (map (fn [admin]
             (let [profile (profile-repo/get-admin-profile profile-repository (:id admin))]
               (-> admin
                   (dissoc :password_hash)
                   (assoc :profile profile))))
           admins)))

  (get-admin-detail [_ admin-id]
    (when-let [user (user-repo/find-by-id user-repository admin-id)]
      (when (= "admin" (:role user))
        (let [profile (profile-repo/get-admin-profile profile-repository admin-id)
              permissions (perm-repo/get-admin-permissions permission-repository admin-id)
              is-super (= 1 (:is_super_admin profile))]
          (-> user
              (dissoc :password_hash)
              (assoc :profile profile
                     :is_super_admin is-super
                     :permissions (if is-super :all (map :code permissions))))))))

  (update-admin-permissions [_ admin-id permission-ids updated-by-id]
    ;; Hanya super admin yang bisa update permissions
    (when-not (perm-repo/is-super-admin? permission-repository updated-by-id)
      (throw (ex-info "Only super admin can update permissions" {:type :forbidden})))

    ;; Tidak bisa update permissions super admin
    (when (perm-repo/is-super-admin? permission-repository admin-id)
      (throw (ex-info "Cannot modify super admin permissions" {:type :forbidden})))

    ;; Revoke semua permissions lama
    (perm-repo/revoke-all-permissions! permission-repository admin-id)

    ;; Grant permissions baru
    (doseq [perm-id permission-ids]
      (perm-repo/grant-permission! permission-repository admin-id perm-id updated-by-id))

    {:message "Permissions updated successfully"})

  (deactivate-admin [_ admin-id updated-by-id]
    ;; Hanya super admin yang bisa deactivate admin
    (when-not (perm-repo/is-super-admin? permission-repository updated-by-id)
      (throw (ex-info "Only super admin can deactivate admins" {:type :forbidden})))

    ;; Tidak bisa deactivate super admin
    (when (perm-repo/is-super-admin? permission-repository admin-id)
      (throw (ex-info "Cannot deactivate super admin" {:type :forbidden})))

    (user-repo/update! user-repository admin-id {:is_active false})
    {:message "Admin deactivated successfully"})

  (list-all-permissions [_]
    (perm-repo/find-all-permissions permission-repository))

  (get-admin-permissions [_ admin-id]
    (let [is-super (perm-repo/is-super-admin? permission-repository admin-id)]
      (if is-super
        {:is_super_admin true
         :permissions :all}
        {:is_super_admin false
         :permissions (perm-repo/get-admin-permissions permission-repository admin-id)}))))

(defn create-service [user-repository profile-repository permission-repository]
  (->DefaultAdminService user-repository profile-repository permission-repository))
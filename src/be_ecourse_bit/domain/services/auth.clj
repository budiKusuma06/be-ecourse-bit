(ns be-ecourse-bit.domain.services.auth
  (:require [buddy.hashers :as hashers]
            [clj-time.core :as time]
            [clj-time.coerce :as tc]
            [be-ecourse-bit.domain.repositories.user :as user-repo]
            [be-ecourse-bit.domain.repositories.profile :as profile-repo]
            [be-ecourse-bit.domain.repositories.permission :as perm-repo]))

(defprotocol AuthService
  (register-student [this student-data])
  (register-student-oauth [this oauth-data])
  (create-admin [this admin-data created-by-id])
  (authenticate-email [this email password])
  (authenticate-google [this google-id])
  (create-session-data [this user]))

(defrecord DefaultAuthService [user-repository profile-repository permission-repository]
  AuthService
  (register-student [_ student-data]
    (let [user-data {:email (:email student-data)
                     :password_hash (hashers/derive (:password student-data))
                     :google_id nil
                     :role "student"
                     :is_active true}
          user (user-repo/create! user-repository user-data)
          profile-data {:user_id (:id user)
                        :full_name (:full_name student-data)
                        :phone (:phone student-data)}]
      (profile-repo/create-student-profile! profile-repository profile-data)
      (dissoc user :password_hash)))

  (register-student-oauth [_ oauth-data]
    (let [user-data {:email (:email oauth-data)
                     :password_hash nil
                     :google_id (:google_id oauth-data)
                     :role "student"
                     :is_active true}
          user (user-repo/create! user-repository user-data)
          profile-data {:user_id (:id user)
                        :full_name (:full_name oauth-data)
                        :avatar_url (:avatar_url oauth-data)}]
      (profile-repo/create-student-profile! profile-repository profile-data)
      (dissoc user :password_hash)))

  (create-admin [_ admin-data created-by-id]
    (when-not (perm-repo/is-super-admin? permission-repository created-by-id)
      (throw (ex-info "Only super admin can create new admins" {:type :forbidden})))

    (let [user-data {:email (:email admin-data)
                     :password_hash (hashers/derive (:password admin-data))
                     :google_id nil
                     :role "admin"
                     :is_active true}
          user (user-repo/create! user-repository user-data)
          profile-data {:user_id (:id user)
                        :full_name (:full_name admin-data)
                        :department (:department admin-data)
                        :position (:position admin-data)
                        :is_super_admin false}]
      (profile-repo/create-admin-profile! profile-repository profile-data)

      (when-let [permission-ids (:permission_ids admin-data)]
        (doseq [perm-id permission-ids]
          (perm-repo/grant-permission! permission-repository
                                       (:id user)
                                       perm-id
                                       created-by-id)))

      (dissoc user :password_hash)))

  (authenticate-email [_ email password]
    ;; Login dengan email/password (student atau admin)
    (when-let [user (user-repo/find-by-email user-repository email)]
      (when (and (:password_hash user)
                 (hashers/check password (:password_hash user))
                 (:is_active user))
        user)))

  (authenticate-google [_ google-id]
    ;; Login via Google OAuth (hanya student)
    (when-let [user (user-repo/find-by-google-id user-repository google-id)]
      (when (and (= "student" (:role user))
                 (:is_active user))
        user)))

  (create-session-data [this user]
    (let [base-session {:id (:id user)
                        :email (:email user)
                        :role (:role user)
                        :login-time (tc/to-long (time/now))}]
      (if (= "admin" (:role user))
        ;; Admin session: tambahkan permissions
        (let [is-super (perm-repo/is-super-admin? permission-repository (:id user))
              permissions (if is-super
                            :all
                            (map :code (perm-repo/get-admin-permissions permission-repository (:id user))))]
          (assoc base-session
                 :is_super_admin is-super  ; Bug disini, harus boolean bukan int
                 :permissions permissions))
        ;; Student session: data minimal
        base-session))))

(defn create-service [user-repository profile-repository permission-repository]
  (->DefaultAuthService user-repository profile-repository permission-repository))
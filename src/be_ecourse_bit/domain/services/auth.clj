(ns be-ecourse-bit.domain.services.auth
  (:require [buddy.hashers :as hashers]
            [clj-time.core :as time]
            [clj-time.coerce :as tc]
            [be-ecourse-bit.domain.repositories.user :as user-repo]))

(defprotocol AuthService
  (register-user [this user-data])
  (authenticate [this username password])
  (create-session-data [this user]))

(defrecord DefaultAuthService [user-repository]
  AuthService
  (register-user [_ user-data]
    (let [user-with-hash (-> user-data
                             (assoc :password_hash (hashers/derive (:password user-data))
                                    :role (or (:role user-data) "student"))
                             (dissoc :password))]
      (user-repo/create! user-repository user-with-hash)))

  (authenticate [_ username password]
    (when-let [user (user-repo/find-by-username user-repository username)]
      (when (hashers/check password (:password_hash user))
        user)))

  (create-session-data [_ user]
    {:id (:id user)
     :username (:username user)
     :email (:email user)
     :role (:role user)
     :login-time (tc/to-long (time/now))}))

(defn create-service [user-repository]
  (->DefaultAuthService user-repository))
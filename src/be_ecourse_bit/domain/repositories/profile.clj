(ns be-ecourse-bit.domain.repositories.profile
  (:require [next.jdbc.sql :as sql]
            [next.jdbc.result-set :as rs]))

(defprotocol ProfileRepository
  (get-student-profile [this user-id])
  (get-admin-profile [this user-id])
  (create-student-profile! [this profile])
  (create-admin-profile! [this profile])
  (update-student-profile! [this user-id profile])
  (update-admin-profile! [this user-id profile]))

(defrecord JdbcProfileRepository [db]
  ProfileRepository
  (get-student-profile [_ user-id]
    (first (sql/query db
                      ["SELECT * FROM student_profiles WHERE user_id = ?" user-id]
                      {:builder-fn rs/as-unqualified-lower-maps})))

  (get-admin-profile [_ user-id]
    (first (sql/query db
                      ["SELECT * FROM admin_profiles WHERE user_id = ?" user-id]
                      {:builder-fn rs/as-unqualified-lower-maps})))

  (create-student-profile! [_ profile]
    (sql/insert! db :student_profiles profile))

  (create-admin-profile! [_ profile]
    (sql/insert! db :admin_profiles profile))

  (update-student-profile! [_ user-id profile]
    (sql/update! db :student_profiles profile {:user_id user-id}))

  (update-admin-profile! [_ user-id profile]
    (sql/update! db :admin_profiles profile {:user_id user-id})))

(defn create-repository [db]
  (->JdbcProfileRepository db))
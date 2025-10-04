(ns be-ecourse-bit.domain.repositories.user
  (:require [next.jdbc.sql :as sql]
            [next.jdbc :as jdbc]
            [next.jdbc.result-set :as rs]))

(defprotocol UserRepository
  (find-by-email [this email])
  (find-by-google-id [this google-id])
  (find-by-id [this id])
  (find-all [this])
  (create! [this user])
  (update! [this id user])
  (get-user-with-profile [this id])
  (get-admin-permissions [this admin-id]))

(defrecord JdbcUserRepository [db]
  UserRepository
  (find-by-email [_ email]
    (first (sql/query db
                      ["SELECT * FROM users WHERE email = ?" email]
                      {:builder-fn rs/as-unqualified-lower-maps})))

  (find-by-google-id [_ google-id]
    (first (sql/query db
                      ["SELECT * FROM users WHERE google_id = ?" google-id]
                      {:builder-fn rs/as-unqualified-lower-maps})))

  (find-by-id [_ id]
    (first (sql/query db
                      ["SELECT * FROM users WHERE id = ?" id]
                      {:builder-fn rs/as-unqualified-lower-maps})))

  (find-all [_]
    (sql/query db
               ["SELECT id, email, role, is_active, created_at FROM users"]
               {:builder-fn rs/as-unqualified-lower-maps}))

  (create! [_ user]
    (jdbc/with-transaction [tx db]
      (jdbc/execute-one! tx
                         ["INSERT INTO users (email, password_hash, google_id, role, is_active) 
                          VALUES (?, ?, ?, ?, ?)"
                          (:email user)
                          (:password_hash user)
                          (:google_id user)
                          (:role user)
                          (get user :is_active true)])
      (let [generated-id (:GENERATED_KEY (jdbc/execute-one! tx ["SELECT LAST_INSERT_ID() AS GENERATED_KEY"]))]
        (assoc user :id generated-id))))

  (update! [_ id user]
    (sql/update! db :users user {:id id}))

  (get-user-with-profile [_ id]
    (let [user (first (sql/query db
                                 ["SELECT * FROM users WHERE id = ?" id]
                                 {:builder-fn rs/as-unqualified-lower-maps}))]
      (when user
        (if (= "student" (:role user))
          ;; Get student profile
          (let [profile (first (sql/query db
                                          ["SELECT * FROM student_profiles WHERE user_id = ?" id]
                                          {:builder-fn rs/as-unqualified-lower-maps}))]
            (assoc user :profile profile))
          ;; Get admin profile
          (let [profile (first (sql/query db
                                          ["SELECT * FROM admin_profiles WHERE user_id = ?" id]
                                          {:builder-fn rs/as-unqualified-lower-maps}))]
            (assoc user :profile profile))))))

  (get-admin-permissions [_ admin-id]
    (sql/query db
               ["SELECT p.* FROM permissions p
                 INNER JOIN admin_permissions ap ON p.id = ap.permission_id
                 WHERE ap.admin_id = ?" admin-id]
               {:builder-fn rs/as-unqualified-lower-maps})))

(defn create-repository [db]
  (->JdbcUserRepository db))
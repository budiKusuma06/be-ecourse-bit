(ns be-ecourse-bit.domain.repositories.user
  (:require [next.jdbc.sql :as sql]
            [next.jdbc.result-set :as rs]))

(defprotocol UserRepository
  (find-by-username [this username])
  (find-by-id [this id])
  (find-all [this])
  (create! [this user]))

(defrecord JdbcUserRepository [db]
  UserRepository
  (find-by-username [_ username]
    (first (sql/query db
                      ["SELECT * FROM users WHERE username = ?" username]
                      {:builder-fn rs/as-unqualified-lower-maps})))

  (find-by-id [_ id]
    (first (sql/query db
                      ["SELECT * FROM users WHERE id = ?" id]
                      {:builder-fn rs/as-unqualified-lower-maps})))

  (find-all [_]
    (sql/query db
               ["SELECT id, username, email, role, created_at FROM users"]
               {:builder-fn rs/as-unqualified-lower-maps}))

  (create! [_ user]
    (sql/insert! db :users user)))

(defn create-repository [db]
  (->JdbcUserRepository db))
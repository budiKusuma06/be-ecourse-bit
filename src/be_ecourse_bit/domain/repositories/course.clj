(ns be-ecourse-bit.domain.repositories.course
  (:require [next.jdbc :as jdbc]
            [next.jdbc.sql :as sql]
            [next.jdbc.result-set :as rs]))

(defprotocol CourseRepository
  (find-all [this])
  (find-by-id [this id])
  (create! [this course])
  (update! [this id course])
  (delete! [this id]))

(defrecord JdbcCourseRepository [db]
  CourseRepository
  (find-all [_]
    (sql/query db ["SELECT * FROM courses"]
               {:builder-fn rs/as-unqualified-lower-maps}))

  (find-by-id [_ id]
    (sql/get-by-id db :courses id {:builder-fn rs/as-unqualified-lower-maps}))

  (create! [_ course]
    (jdbc/with-transaction [tx db]
      (sql/insert! tx :courses course)
      (jdbc/execute-one! tx ["SELECT LAST_INSERT_ID() AS id"])))

  (update! [_ id course]
    (sql/update! db :courses course {:id id}))

  (delete! [_ id]
    (sql/delete! db :courses ["id = ?" id])))

(defn create-repository [db]
  (->JdbcCourseRepository db))
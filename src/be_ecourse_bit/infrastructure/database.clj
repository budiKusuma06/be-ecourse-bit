(ns be-ecourse-bit.infrastructure.database
  (:require [next.jdbc :as jdbc]
            [next.jdbc.sql :as sql]
            [buddy.hashers :as hashers]))

(defn create-datasource [config]
  (jdbc/get-datasource config))

(defn test-connection [datasource]
  (try
    (jdbc/execute-one! datasource ["SELECT 1"])
    true
    (catch Exception e
      (println "Database connection failed:" (.getMessage e))
      false)))

(defn create-database! [server-config dbname]
  (let [server-ds (create-datasource server-config)]
    (jdbc/execute! server-ds [(format "CREATE DATABASE IF NOT EXISTS %s" dbname)])))

(defn setup-database [config]
  (let [server-config (dissoc config :dbname)
        _ (create-database! server-config (:dbname config))
        db (create-datasource config)]
    (when (test-connection db)
      (println "→ Database connection successful")
      db)))

(defn create-tables! [db]
  (jdbc/execute! db ["
    CREATE TABLE IF NOT EXISTS courses (
      id INT AUTO_INCREMENT PRIMARY KEY,
      title VARCHAR(255) NOT NULL,
      description TEXT NOT NULL,
      instructor VARCHAR(255) NOT NULL,
      price DECIMAL(10,2) NOT NULL,
      created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
      updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
    )"])
  (println "→ Table 'courses' created")

  (jdbc/execute! db ["
    CREATE TABLE IF NOT EXISTS users (
      id INT AUTO_INCREMENT PRIMARY KEY,
      username VARCHAR(50) UNIQUE NOT NULL,
      email VARCHAR(255) UNIQUE NOT NULL,
      password_hash VARCHAR(255) NOT NULL,
      role ENUM('admin', 'instructor', 'student') DEFAULT 'student',
      created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
      updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
    )"])
  (println "→ Table 'users' created"))

(defn seed-data! [db]
  (let [course-count (:count (jdbc/execute-one! db ["SELECT COUNT(*) AS count FROM courses"]))
        user-count (:count (jdbc/execute-one! db ["SELECT COUNT(*) AS count FROM users"]))]

    (when (zero? course-count)
      (println "→ Seeding courses...")
      (sql/insert-multi! db :courses
                         [{:title "Clojure Programming Dasar"
                           :description "Menguasai dasar-dasar pemrograman Clojure untuk pemula"
                           :instructor "Budi Santoso"
                           :price 299000.0}
                          {:title "Web Development dengan Reitit"
                           :description "Membangun REST API profesional menggunakan Reitit dan Ring"
                           :instructor "Ani Rahmawati"
                           :price 499000.0}]))

    (when (zero? user-count)
      (println "→ Seeding users...")
      (sql/insert-multi! db :users
                         [{:username "admin"
                           :email "admin@ecourse.com"
                           :password_hash (hashers/derive "admin123")
                           :role "admin"}
                          {:username "instructor"
                           :email "instructor@ecourse.com"
                           :password_hash (hashers/derive "instructor123")
                           :role "instructor"}
                          {:username "student"
                           :email "student@ecourse.com"
                           :password_hash (hashers/derive "student123")
                           :role "student"}])
      (println "→ Default credentials:")
      (println "   Admin: admin/admin123")
      (println "   Instructor: instructor/instructor123")
      (println "   Student: student/student123"))))
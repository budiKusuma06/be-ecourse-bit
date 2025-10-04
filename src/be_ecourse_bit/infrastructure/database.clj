(ns be-ecourse-bit.infrastructure.database
  (:require [next.jdbc :as jdbc]
            [next.jdbc.sql :as sql]
            [next.jdbc.result-set :as rs]
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
  ;; Users table - auth data untuk semua user
  (jdbc/execute! db ["
    CREATE TABLE IF NOT EXISTS users (
      id INT AUTO_INCREMENT PRIMARY KEY,
      email VARCHAR(255) UNIQUE NOT NULL,
      password_hash VARCHAR(255),
      google_id VARCHAR(255) UNIQUE,
      role ENUM('student', 'admin') NOT NULL,
      is_active BOOLEAN DEFAULT TRUE,
      created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
      updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
      INDEX idx_email (email),
      INDEX idx_google_id (google_id),
      INDEX idx_role (role)
    )"])
  (println "→ Table 'users' created")

  ;; Student profiles
  (jdbc/execute! db ["
    CREATE TABLE IF NOT EXISTS student_profiles (
      id INT AUTO_INCREMENT PRIMARY KEY,
      user_id INT UNIQUE NOT NULL,
      full_name VARCHAR(255),
      phone VARCHAR(20),
      date_of_birth DATE,
      bio TEXT,
      avatar_url VARCHAR(500),
      created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
      updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
      FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
    )"])
  (println "→ Table 'student_profiles' created")

  ;; Admin profiles
  (jdbc/execute! db ["
    CREATE TABLE IF NOT EXISTS admin_profiles (
      id INT AUTO_INCREMENT PRIMARY KEY,
      user_id INT UNIQUE NOT NULL,
      full_name VARCHAR(255) NOT NULL,
      department VARCHAR(100),
      position VARCHAR(100),
      phone VARCHAR(20),
      avatar_url VARCHAR(500),
      is_super_admin BOOLEAN DEFAULT FALSE,
      created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
      updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
      FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
    )"])
  (println "→ Table 'admin_profiles' created")

  ;; Master permissions
  (jdbc/execute! db ["
    CREATE TABLE IF NOT EXISTS permissions (
      id INT AUTO_INCREMENT PRIMARY KEY,
      code VARCHAR(50) UNIQUE NOT NULL,
      name VARCHAR(100) NOT NULL,
      description TEXT,
      category VARCHAR(50),
      created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
      INDEX idx_code (code),
      INDEX idx_category (category)
    )"])
  (println "→ Table 'permissions' created")

  ;; Admin permissions (junction table)
  (jdbc/execute! db ["
    CREATE TABLE IF NOT EXISTS admin_permissions (
      id INT AUTO_INCREMENT PRIMARY KEY,
      admin_id INT NOT NULL,
      permission_id INT NOT NULL,
      granted_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
      granted_by INT,
      UNIQUE KEY unique_admin_permission (admin_id, permission_id),
      FOREIGN KEY (admin_id) REFERENCES users(id) ON DELETE CASCADE,
      FOREIGN KEY (permission_id) REFERENCES permissions(id) ON DELETE CASCADE,
      FOREIGN KEY (granted_by) REFERENCES users(id) ON DELETE SET NULL,
      INDEX idx_admin_id (admin_id),
      INDEX idx_permission_id (permission_id)
    )"])
  (println "→ Table 'admin_permissions' created")

  ;; Courses table (tetap sama)
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
  (println "→ Table 'courses' created"))

(defn seed-permissions! [db]
  "Seed master permissions"
  (let [permission-count (:count (jdbc/execute-one! db ["SELECT COUNT(*) AS count FROM permissions"]))]
    (when (zero? permission-count)
      (println "→ Seeding permissions...")
      (sql/insert-multi! db :permissions
                         [{:code "manage_users" :name "Manage Users"
                           :description "Create, read, update, delete users" :category "users"}
                          {:code "manage_courses" :name "Manage Courses"
                           :description "Create, read, update, delete courses" :category "courses"}
                          {:code "view_courses" :name "View Courses"
                           :description "View course list and details" :category "courses"}
                          {:code "manage_admins" :name "Manage Admins"
                           :description "Create admins and assign permissions" :category "admins"}
                          {:code "view_analytics" :name "View Analytics"
                           :description "View system analytics and reports" :category "analytics"}
                          {:code "manage_settings" :name "Manage Settings"
                           :description "Manage system settings" :category "settings"}]))))

(defn seed-data! [db]
  (let [user-count (:count (jdbc/execute-one! db ["SELECT COUNT(*) AS count FROM users"]))
        course-count (:count (jdbc/execute-one! db ["SELECT COUNT(*) AS count FROM courses"]))]

    ;; Seed permissions first
    (seed-permissions! db)

    (when (zero? user-count)
      (println "→ Seeding users...")

      (jdbc/with-transaction [tx db]
        ;; Create super admin
        (jdbc/execute-one! tx
                           ["INSERT INTO users (email, password_hash, role, is_active) VALUES (?, ?, ?, ?)"
                            "superadmin@ecourse.com"
                            (hashers/derive "superadmin123")
                            "admin"
                            true])

        (let [super-admin-id (:GENERATED_KEY (jdbc/execute-one! tx ["SELECT LAST_INSERT_ID() AS GENERATED_KEY"]))]

          (println "  Super admin ID:" super-admin-id)

          ;; Create super admin profile
          (jdbc/execute-one! tx
                             ["INSERT INTO admin_profiles (user_id, full_name, department, position, is_super_admin) VALUES (?, ?, ?, ?, ?)"
                              super-admin-id
                              "Super Administrator"
                              "System"
                              "Super Admin"
                              true])

          ;; Grant all permissions to super admin
          (let [all-permissions (jdbc/execute! tx ["SELECT id FROM permissions"] {:builder-fn rs/as-unqualified-lower-maps})]
            (println "  Found" (count all-permissions) "permissions")
            (doseq [perm all-permissions]
              (let [perm-id (:id perm)]
                (println "    Granting permission ID:" perm-id)
                (jdbc/execute-one! tx
                                   ["INSERT INTO admin_permissions (admin_id, permission_id, granted_by) VALUES (?, ?, ?)"
                                    super-admin-id
                                    perm-id
                                    super-admin-id])))))

        ;; Create regular admin
        (jdbc/execute-one! tx
                           ["INSERT INTO users (email, password_hash, role, is_active) VALUES (?, ?, ?, ?)"
                            "admin@ecourse.com"
                            (hashers/derive "admin123")
                            "admin"
                            true])

        (let [admin-id (:GENERATED_KEY (jdbc/execute-one! tx ["SELECT LAST_INSERT_ID() AS GENERATED_KEY"]))
              course-perms (jdbc/execute! tx
                                          ["SELECT id FROM permissions WHERE code IN ('view_courses', 'manage_courses')"]
                                          {:builder-fn rs/as-unqualified-lower-maps})]

          (println "  Regular admin ID:" admin-id)

          ;; Create admin profile
          (jdbc/execute-one! tx
                             ["INSERT INTO admin_profiles (user_id, full_name, department, position, is_super_admin) VALUES (?, ?, ?, ?, ?)"
                              admin-id
                              "Regular Admin"
                              "Education"
                              "Course Manager"
                              false])

          ;; Grant course permissions only
          (doseq [perm course-perms]
            (let [perm-id (:id perm)]
              (println "    Granting course permission ID:" perm-id)
              (jdbc/execute-one! tx
                                 ["INSERT INTO admin_permissions (admin_id, permission_id) VALUES (?, ?)"
                                  admin-id
                                  perm-id]))))

        ;; Create student users
        (jdbc/execute-one! tx
                           ["INSERT INTO users (email, password_hash, role, is_active) VALUES (?, ?, ?, ?)"
                            "student@ecourse.com"
                            (hashers/derive "student123")
                            "student"
                            true])

        (let [student-id (:GENERATED_KEY (jdbc/execute-one! tx ["SELECT LAST_INSERT_ID() AS GENERATED_KEY"]))]

          (println "  Student ID:" student-id)

          ;; Create student profile
          (jdbc/execute-one! tx
                             ["INSERT INTO student_profiles (user_id, full_name, phone) VALUES (?, ?, ?)"
                              student-id
                              "Student User"
                              "08123456789"]))

        ;; Create Google OAuth student example
        (jdbc/execute-one! tx
                           ["INSERT INTO users (email, google_id, role, is_active) VALUES (?, ?, ?, ?)"
                            "googlestudent@gmail.com"
                            "google_123456789"
                            "student"
                            true])

        (let [google-student-id (:GENERATED_KEY (jdbc/execute-one! tx ["SELECT LAST_INSERT_ID() AS GENERATED_KEY"]))]

          (println "  Google student ID:" google-student-id)

          ;; Create student profile
          (jdbc/execute-one! tx
                             ["INSERT INTO student_profiles (user_id, full_name, phone) VALUES (?, ?, ?)"
                              google-student-id
                              "Google Student"
                              "08987654321"])))

      (println "→ Default credentials:")
      (println "   Super Admin: superadmin@ecourse.com/superadmin123")
      (println "   Admin: admin@ecourse.com/admin123")
      (println "   Student: student@ecourse.com/student123")
      (println "   Google Student: googlestudent@gmail.com (OAuth only)"))

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
                           :price 499000.0}]))))
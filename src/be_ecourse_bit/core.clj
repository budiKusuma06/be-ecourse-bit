(ns be-ecourse-bit.core
  (:require [org.httpkit.server :as http]
            [reitit.ring :as reitit]
            [muuntaja.core :as m]
            [muuntaja.middleware :as muuntaja-middleware]
            [malli.core :as malli]
            [malli.error :as me]
            [mount.core :refer [defstate]]
            [next.jdbc :as jdbc]
            [next.jdbc.sql :as sql]
            [next.jdbc.result-set :as rs]
            [buddy.sign.jwt :as jwt]
            [buddy.hashers :as hashers]
            [clj-time.core :as time]
            [clj-time.coerce :as tc]
            [clojure.string :as str]))

;; JWT Secret Key (gunakan environment variable di production)
(def jwt-secret "your-very-secret-jwt-key-change-in-production")

;; Konfigurasi database
(def db-config
  {:dbtype "mysql"
   :host "localhost"
   :port 3306
   :dbname "ecourse_db"
   :user "root"
   :password "root"
   :allowPublicKeyRetrieval true
   :useSSL false
   :serverTimezone "UTC"
   :nullCatalogMeansCurrent false})

;; Fungsi untuk setup database
(defn setup-database []
  (let [server-config (dissoc db-config :dbname)
        server-ds (jdbc/get-datasource server-config)]

    (try
      (jdbc/execute-one! server-ds ["SELECT 1"])
      (println "→ Connected to MySQL server successfully")

      (try
        (jdbc/execute! server-ds [(format "CREATE DATABASE IF NOT EXISTS %s" (:dbname db-config))])
        (println "→ Database created or already exists")
        (catch Exception e
          (println "→ Warning: Failed to create database:" (.getMessage e))))

      (let [db (jdbc/get-datasource db-config)]

        ;; Buat tabel courses
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
        (println "→ Table 'courses' created or already exists")

        ;; Buat tabel users
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
        (println "→ Table 'users' created or already exists")

        ;; Seed data courses jika kosong
        (when (zero? (-> (jdbc/execute-one! db ["SELECT COUNT(*) AS count FROM courses"])
                         :count))
          (println "→ Seeding initial courses data")
          (sql/insert-multi! db :courses
                             [{:title "Clojure Programming Dasar"
                               :description "Menguasai dasar-dasar pemrograman Clojure untuk pemula"
                               :instructor "Budi Santoso"
                               :price 299000.0}
                              {:title "Web Development dengan Reitit"
                               :description "Membangun REST API profesional menggunakan Reitit dan Ring"
                               :instructor "Ani Rahmawati"
                               :price 499000.0}])
          (println "→ Sample courses inserted"))

        ;; Seed admin user jika belum ada
        (when (zero? (-> (jdbc/execute-one! db ["SELECT COUNT(*) AS count FROM users"])
                         :count))
          (println "→ Creating default admin user")
          (sql/insert! db :users
                       {:username "admin"
                        :email "admin@ecourse.com"
                        :password_hash (hashers/derive "admin123")
                        :role "admin"})
          (println "→ Default admin created (username: admin, password: admin123)"))

        db)

      (catch Exception e
        (println "→ Error setting up database:" (.getMessage e))
        (throw e)))))

;; State untuk koneksi database
(defstate db-connection
  :start (do
           (println "→ Initializing MySQL database setup...")
           (setup-database))
  :stop (println "→ Database connection closed"))

;; Skema validasi
(def CourseSchema
  [:map
   [:title [:string {:min 3}]]
   [:description [:string {:min 10}]]
   [:instructor [:string {:min 2}]]
   [:price [:double {:min 0}]]])

(def UserRegistrationSchema
  [:map
   [:username [:string {:min 3 :max 50}]]
   [:email [:string {:min 5}]]
   [:password [:string {:min 6}]]
   [:role {:optional true} [:enum "admin" "instructor" "student"]]])

(def LoginSchema
  [:map
   [:username [:string]]
   [:password [:string]]])

;; Utility functions
(defn normalize-price [course]
  (if (number? (:price course))
    (assoc course :price (double (:price course)))
    course))

(defn convert-bigdecimals [course]
  (if (instance? java.math.BigDecimal (:price course))
    (assoc course :price (double (:price course)))
    course))

;; JWT functions
(defn generate-token [user]
  (let [claims {:user-id (:id user)
                :username (:username user)
                :role (:role user)
                :exp (tc/to-long (time/plus (time/now) (time/hours 24)))}]
    (jwt/sign claims jwt-secret)))

(defn verify-token [token]
  (try
    (jwt/unsign token jwt-secret)
    (catch Exception e
      nil)))

(defn extract-token [request]
  (when-let [auth-header (get-in request [:headers "authorization"])]
    (when (str/starts-with? auth-header "Bearer ")
      (subs auth-header 7))))

;; Authentication middleware
(defn wrap-auth [handler & [opts]]
  (let [required-roles (set (:roles opts))]
    (fn [request]
      (if-let [token (extract-token request)]
        (if-let [claims (verify-token token)]
          (let [user-role (:role claims)]
            (if (or (empty? required-roles)
                    (contains? required-roles user-role)
                    (= "admin" user-role)) ; admin bisa akses semua
              (handler (assoc request :user claims))
              {:status 403
               :body {:error "Insufficient permissions"}}))
          {:status 401
           :body {:error "Invalid or expired token"}})
        {:status 401
         :body {:error "Authorization token required"}}))))

;; Auth handlers
(defn register [request]
  (let [input (:body-params request)
        error (malli/explain UserRegistrationSchema input)]
    (if error
      {:status 400
       :body {:error (me/humanize error)}}
      (try
        (let [user-data (-> input
                            (assoc :password_hash (hashers/derive (:password input))
                                   :role (or (:role input) "student"))
                            (dissoc :password))]
          (sql/insert! db-connection :users user-data)
          {:status 201
           :body {:message "User registered successfully"
                  :username (:username input)}})
        (catch Exception e
          (println "Registration error:" (.getMessage e))
          (if (str/includes? (.getMessage e) "Duplicate entry")
            {:status 409
             :body {:error "Username or email already exists"}}
            {:status 500
             :body {:error "Registration failed"}}))))))

;; Debug function untuk melihat users
(defn debug-users [_]
  (let [users (sql/query db-connection
                         ["SELECT id, username, email, role, created_at FROM users"]
                         {:builder-fn rs/as-unqualified-lower-maps})]
    {:status 200
     :body users}))

;; Debug function khusus untuk cek password hash
(defn debug-user-detail [request]
  (let [username (-> request :path-params :username)
        user (first (sql/query db-connection
                               ["SELECT * FROM users WHERE username = ?" username]
                               {:builder-fn rs/as-unqualified-lower-maps}))]
    {:status 200
     :body (if user
             {:found true
              :username (:username user)
              :email (:email user)
              :role (:role user)
              :password_hash_length (count (:password_hash user))}
             {:found false})}))

(defn login [request]
  (let [input (:body-params request)
        error (malli/explain LoginSchema input)]
    (if error
      {:status 400
       :body {:error (me/humanize error)}}
      (let [user (first (sql/query db-connection
                                   ["SELECT * FROM users WHERE username = ?" (:username input)]
                                   {:builder-fn rs/as-unqualified-lower-maps}))]
        (println "Debug - Found user:" user)
        (println "Debug - Input password:" (:password input))
        (if user
          (do
            (println "Debug - Stored hash:" (:password_hash user))
            (if (hashers/check (:password input) (:password_hash user))
              {:status 200
               :body {:message "Login successful"
                      :token (generate-token user)
                      :user {:id (:id user)
                             :username (:username user)
                             :email (:email user)
                             :role (:role user)}}}
              {:status 401
               :body {:error "Invalid credentials"}}))
          {:status 401
           :body {:error "User not found"}})))))

(defn get-profile [request]
  (let [user-id (get-in request [:user :user-id])
        user (first (sql/query db-connection
                               ["SELECT * FROM users WHERE id = ?" user-id]
                               {:builder-fn rs/as-unqualified-lower-maps}))]
    (if user
      {:status 200
       :body (dissoc user :password_hash)}
      {:status 404
       :body {:error "User not found"}})))

;; Course handlers (updated with auth)
(defn get-courses [_]
  (let [courses (->> (sql/query db-connection ["SELECT * FROM courses"])
                     (map convert-bigdecimals))]
    {:status 200
     :body courses}))

(defn create-course [request]
  (let [input (normalize-price (:body-params request))
        error (malli/explain CourseSchema input)]
    (if error
      {:status 400
       :body {:error (me/humanize error)}}
      (jdbc/with-transaction [tx db-connection]
        (sql/insert! tx :courses input)
        (let [id (-> (jdbc/execute-one! tx ["SELECT LAST_INSERT_ID() AS id"])
                     :id
                     int)]
          {:status 201
           :body (convert-bigdecimals (assoc input :id id))})))))

(defn get-course [request]
  (let [id (Integer/parseInt (-> request :path-params :id))
        course (->> (sql/get-by-id db-connection :courses id)
                    convert-bigdecimals)]
    (if course
      {:status 200
       :body course}
      {:status 404
       :body {:error "Course not found"}})))

(defn update-course [request]
  (let [id (Integer/parseInt (-> request :path-params :id))
        input (normalize-price (:body-params request))
        error (malli/explain CourseSchema input)]
    (if error
      {:status 400
       :body {:error (me/humanize error)}}
      (if (sql/get-by-id db-connection :courses id)
        (do
          (sql/update! db-connection :courses (dissoc input :id) {:id id})
          {:status 200
           :body (convert-bigdecimals (assoc input :id id))})
        {:status 404
         :body {:error "Course not found"}}))))

(defn delete-course [request]
  (let [id (Integer/parseInt (-> request :path-params :id))]
    (if (sql/delete! db-connection :courses ["id = ?" id])
      {:status 204}
      {:status 404
       :body {:error "Course not found"}})))

;; Router configuration
(def app
  (reitit/ring-handler
   (reitit/router
    [;; Public routes
     ["/auth/register" {:post {:handler register}}]
     ["/auth/login" {:post {:handler login}}]
     ["/courses" {:get {:handler get-courses}}]
     ["/courses/:id" {:get {:handler get-course}}]

     ;; Debug route (test)
     ["/debug/users" {:get {:handler debug-users}}]
     ["/debug/users/:username" {:get {:handler debug-user-detail}}]

     ;; Protected routes
     ["/auth/profile" {:get {:handler (wrap-auth get-profile)}}]
     ["/admin"
      ["/courses" {:post {:handler (wrap-auth create-course {:roles #{"admin" "instructor"}})}}]
      ["/courses/:id"
       {:put {:handler (wrap-auth update-course {:roles #{"admin" "instructor"}})}
        :delete {:handler (wrap-auth delete-course {:roles #{"admin"}})}}]]]
    {:data {:muuntaja m/instance
            :middleware [muuntaja-middleware/wrap-format]}})
   (reitit/create-default-handler)))

;; State untuk HTTP server
(defstate http-server
  :start (do
           (println "→ Starting HTTP server on port 3000")
           (http/run-server #'app {:port 3000}))
  :stop (do
          (println "→ Stopping HTTP server")
          (@http-server :timeout 100)
          (http-server false)))

;; Main function
(defn -main [& args]
  (println "========================================")
  (println "e-Course API with Authentication")
  (println "========================================")
  (println "→ Checking database requirements...")
  (mount.core/start)
  (println "========================================")
  (println "e-Course API server is running")
  (println "→ Access API at: http://localhost:3000")
  (println "→ Default admin: username=admin, password=admin123")
  (println "")
  (println "Available endpoints:")
  (println "  Public:")
  (println "    POST /auth/register - Register new user")
  (println "    POST /auth/login    - Login user")
  (println "    GET  /courses       - List all courses")
  (println "    GET  /courses/:id   - Get course by ID")
  (println "")
  (println "  Protected (requires token):")
  (println "    GET    /auth/profile     - Get user profile")
  (println "    POST   /admin/courses    - Create course (admin/instructor)")
  (println "    PUT    /admin/courses/:id - Update course (admin/instructor)")
  (println "    DELETE /admin/courses/:id - Delete course (admin only)")
  (println "========================================"))
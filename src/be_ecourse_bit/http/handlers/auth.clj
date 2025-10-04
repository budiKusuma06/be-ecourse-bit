(ns be-ecourse-bit.http.handlers.auth
  (:require [be-ecourse-bit.domain.services.auth :as auth-svc]
            [be-ecourse-bit.domain.repositories.user :as user-repo]
            [be-ecourse-bit.domain.validators :as v]
            [be-ecourse-bit.http.middleware :as mw]
            [malli.error :as me]
            [clojure.string :as str]))

;; ============================================
;; Student Registration & Login
;; ============================================

(defn register-student [auth-service]
  (fn [request]
    (let [input (:body-params request)
          error (v/validate v/student-registration-schema input)]
      (if error
        {:status 400 :body {:error (me/humanize error)}}
        (try
          (let [user (auth-svc/register-student auth-service input)]
            {:status 201
             :body {:message "Student registered successfully"
                    :user (dissoc user :password_hash)}})
          (catch Exception e
            (if (str/includes? (.getMessage e) "Duplicate entry")
              {:status 409 :body {:error "Email already exists"}}
              {:status 500 :body {:error "Registration failed"}})))))))

(defn register-student-oauth [auth-service]
  (fn [request]
    (let [input (:body-params request)
          error (v/validate v/student-oauth-registration-schema input)]
      (if error
        {:status 400 :body {:error (me/humanize error)}}
        (try
          (let [user (auth-svc/register-student-oauth auth-service input)]
            {:status 201
             :body {:message "Student registered via OAuth successfully"
                    :user user}})
          (catch Exception e
            (if (str/includes? (.getMessage e) "Duplicate entry")
              {:status 409 :body {:error "Email or Google ID already exists"}}
              {:status 500 :body {:error "Registration failed"}})))))))

;; ============================================
;; Login (Unified for Student & Admin)
;; ============================================

(defn login-email [auth-service]
  (fn [request]
    (let [input (:body-params request)
          error (v/validate v/login-email-schema input)]
      (if error
        {:status 400 :body {:error (me/humanize error)}}
        (if-let [user (auth-svc/authenticate-email auth-service
                                                   (:email input)
                                                   (:password input))]
          (let [session-user (auth-svc/create-session-data auth-service user)]
            {:status 200
             :body {:message "Login successful"
                    :user (dissoc session-user :login-time)}
             :session {:user session-user}})
          {:status 401 :body {:error "Invalid credentials"}})))))

(defn login-google [auth-service]
  (fn [request]
    (let [input (:body-params request)
          error (v/validate v/login-google-schema input)]
      (if error
        {:status 400 :body {:error (me/humanize error)}}
        (if-let [user (auth-svc/authenticate-google auth-service (:google_id input))]
          (let [session-user (auth-svc/create-session-data auth-service user)]
            {:status 200
             :body {:message "Login via Google successful"
                    :user (dissoc session-user :login-time)}
             :session {:user session-user}})
          {:status 401 :body {:error "Invalid Google credentials or not a student"}})))))

;; ============================================
;; Session Management
;; ============================================

(defn logout []
  (fn [_]
    {:status 200
     :body {:message "Logout successful"}
     :session nil}))

(defn session-status []
  (fn [request]
    (let [session-user (mw/get-session-user request)]
      {:status 200
       :body {:authenticated (not (nil? session-user))
              :user (when session-user (dissoc session-user :login-time))}})))

;; ============================================
;; Profile (Student & Admin)
;; ============================================

(defn profile [user-repository]
  (fn [request]
    (let [session-user (mw/get-session-user request)]
      (if session-user
        (if-let [user (user-repo/get-user-with-profile user-repository (:id session-user))]
          {:status 200
           :body (-> user
                     (dissoc :password_hash :google_id)
                     (assoc :is_super_admin (when (= "admin" (:role user))
                                              (get-in user [:profile :is_super_admin]))))}
          {:status 404 :body {:error "User not found"}})
        {:status 401 :body {:error "Not authenticated"}}))))

;; ============================================
;; Admin Creation (Super Admin Only)
;; ============================================

(defn create-admin [auth-service]
  (fn [request]
    (let [session-user (mw/get-session-user request)
          input (:body-params request)
          error (v/validate v/admin-creation-schema input)]
      (if error
        {:status 400 :body {:error (me/humanize error)}}
        (try
          (let [admin (auth-svc/create-admin auth-service input (:id session-user))]
            {:status 201
             :body {:message "Admin created successfully"
                    :admin admin}})
          (catch clojure.lang.ExceptionInfo e
            (let [data (ex-data e)]
              (println "ExceptionInfo creating admin:" (.getMessage e) data)
              (if (= :forbidden (:type data))
                {:status 403 :body {:error (.getMessage e)}}
                {:status 500 :body {:error (.getMessage e)}})))
          (catch Exception e
            (println "Exception creating admin:" (.getMessage e))
            (if (str/includes? (.getMessage e) "Duplicate entry")
              {:status 409 :body {:error "Email already exists"}}
              {:status 500 :body {:error (str "Failed to create admin: " (.getMessage e))}})))))))

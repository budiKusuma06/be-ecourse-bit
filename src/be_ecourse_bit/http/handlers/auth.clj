(ns be-ecourse-bit.http.handlers.auth
  (:require [be-ecourse-bit.domain.services.auth :as auth-svc]
            [be-ecourse-bit.domain.repositories.user :as user-repo]
            [be-ecourse-bit.domain.validators :as v]
            [be-ecourse-bit.http.middleware :as mw]
            [malli.error :as me]
            [clojure.string :as str]))

(defn register [auth-service]
  (fn [request]
    (let [input (:body-params request)
          error (v/validate v/user-registration-schema input)]
      (if error
        {:status 400 :body {:error (me/humanize error)}}
        (try
          (auth-svc/register-user auth-service input)
          {:status 201
           :body {:message "User registered successfully"
                  :username (:username input)}}
          (catch Exception e
            (if (str/includes? (.getMessage e) "Duplicate entry")
              {:status 409 :body {:error "Username or email already exists"}}
              {:status 500 :body {:error "Registration failed"}})))))))

(defn login [auth-service]
  (fn [request]
    (let [input (:body-params request)
          error (v/validate v/login-schema input)]
      (if error
        {:status 400 :body {:error (me/humanize error)}}
        (if-let [user (auth-svc/authenticate auth-service
                                             (:username input)
                                             (:password input))]
          (let [session-user (auth-svc/create-session-data auth-service user)]
            {:status 200
             :body {:message "Login successful"
                    :user (dissoc session-user :login-time)}
             :session {:user session-user}})
          {:status 401 :body {:error "Invalid credentials"}})))))

(defn logout []
  (fn [_]
    {:status 200
     :body {:message "Logout successful"}
     :session nil}))

(defn profile [user-repository]
  (fn [request]
    (let [session-user (mw/get-session-user request)]
      (if session-user
        (if-let [user (user-repo/find-by-id user-repository (:id session-user))]
          {:status 200 :body (dissoc user :password_hash)}
          {:status 404 :body {:error "User not found"}})
        {:status 401 :body {:error "Not authenticated"}}))))

(defn session-status []
  (fn [request]
    (let [session-user (mw/get-session-user request)]
      {:status 200
       :body {:authenticated (not (nil? session-user))
              :user (when session-user (dissoc session-user :login-time))}})))
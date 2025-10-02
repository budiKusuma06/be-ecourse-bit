(ns be-ecourse-bit.http.middleware
  (:require [ring.middleware.session :as session]
            [ring.middleware.cookies :as cookies]))

(defn get-session-user [request]
  (get-in request [:session :user]))

(defn wrap-session [handler session-store]
  (-> handler
      (session/wrap-session
       {:store session-store
        :cookie-name "ecourse-session"
        :cookie-attrs {:max-age (* 24 60 60)
                       :http-only true
                       :secure false
                       :same-site :lax}})
      cookies/wrap-cookies))

(defn wrap-auth
  ([handler] (wrap-auth handler {}))
  ([handler opts]
   (let [required-roles (set (:roles opts))]
     (fn [request]
       (if-let [session-user (get-session-user request)]
         (let [user-role (:role session-user)]
           (if (or (empty? required-roles)
                   (contains? required-roles user-role)
                   (= "admin" user-role))
             (handler (assoc request :user session-user))
             {:status 403
              :body {:error "Insufficient permissions"}}))
         {:status 401
          :body {:error "Please login to access this resource"}})))))
(ns be-ecourse-bit.http.routes.auth
  (:require [be-ecourse-bit.http.handlers.auth :as handlers]
            [be-ecourse-bit.http.middleware :as mw]))

(defn routes [services]
  (let [{:keys [auth-service user-repository]} services]
    [["/auth/register"
      {:post {:handler (handlers/register auth-service)}}]

     ["/auth/login"
      {:post {:handler (handlers/login auth-service)}}]

     ["/auth/logout"
      {:post {:handler (handlers/logout)}}]

     ["/auth/status"
      {:get {:handler (handlers/session-status)}}]

     ["/auth/profile"
      {:get {:handler (mw/wrap-auth (handlers/profile user-repository))}}]]))
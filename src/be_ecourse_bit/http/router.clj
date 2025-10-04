(ns be-ecourse-bit.http.router
  (:require [reitit.ring :as reitit]
            [muuntaja.core :as m]
            [muuntaja.middleware :as muuntaja-middleware]
            [be-ecourse-bit.http.routes.home :as home-routes]
            [be-ecourse-bit.http.routes.auth :as auth-routes]
            [be-ecourse-bit.http.routes.admin :as admin-routes]
            [be-ecourse-bit.http.routes.profile :as profile-routes]
            [be-ecourse-bit.http.routes.course :as course-routes]
            [be-ecourse-bit.http.routes.debug :as debug-routes]
            [muuntaja.core :as muuntaja]))

(defn not-found-handler [_]
  {:status 404
   :headers {"Content-Type" "application/json"}
   :body (muuntaja/encode "application/json"
                          {:error "Route not found"
                           :message "The requested endpoint does not exist"})})

(defn method-not-allowed-handler [_]
  {:status 405
   :headers {"Content-Type" "application/json"}
   :body (muuntaja/encode "application/json"
                          {:error "Method not allowed"
                           :message "HTTP method not supported for this endpoint"})})

(defn create-router [services]
  (reitit/router
   (concat
    (home-routes/routes services)
    (auth-routes/routes services)
    (admin-routes/routes services)
    (profile-routes/routes services)
    (course-routes/routes services)
    (debug-routes/routes services))
   {:data {:muuntaja m/instance
           :middleware [muuntaja-middleware/wrap-format]}}))
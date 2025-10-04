(ns be-ecourse-bit.http.router
  (:require [reitit.ring :as reitit]
            [muuntaja.core :as m]
            [muuntaja.middleware :as muuntaja-middleware]
            [be-ecourse-bit.http.routes.home :as home-routes]
            [be-ecourse-bit.http.routes.auth :as auth-routes]
            [be-ecourse-bit.http.routes.course :as course-routes]
            [be-ecourse-bit.http.routes.debug :as debug-routes]))

(defn create-router [services]
  (reitit/router
   (concat
    (home-routes/routes services)
    (auth-routes/routes services)
    (course-routes/routes services)
    (debug-routes/routes services))
   {:data {:muuntaja m/instance
           :middleware [muuntaja-middleware/wrap-format]}}))
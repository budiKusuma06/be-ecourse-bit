(ns be-ecourse-bit.http.routes.course
  (:require [be-ecourse-bit.http.handlers.course :as handlers]
            [be-ecourse-bit.http.middleware :as mw]))

(defn routes [services]
  (let [{:keys [course-service]} services]
    [["/courses"
      {:get {:handler (handlers/list-courses course-service)}}]

     ["/courses/:id"
      {:get {:handler (handlers/get-course course-service)}}]

     ["/admin/courses"
      {:post {:handler (mw/wrap-auth
                        (handlers/create-course course-service)
                        {:roles #{"admin" "instructor"}})}}]

     ["/admin/courses/:id"
      {:put {:handler (mw/wrap-auth
                       (handlers/update-course course-service)
                       {:roles #{"admin" "instructor"}})}
       :delete {:handler (mw/wrap-auth
                          (handlers/delete-course course-service)
                          {:roles #{"admin"}})}}]]))
(ns be-ecourse-bit.http.routes.course
  (:require [be-ecourse-bit.http.handlers.course :as handlers]
            [be-ecourse-bit.http.middleware :as mw]))

(defn routes [services]
  (let [{:keys [course-service]} services]
    [;; ============================================
     ;; Public Course Access
     ;; ============================================
     ["/courses"
      {:get {:handler (handlers/list-courses course-service)}}]

     ["/courses/:id"
      {:get {:handler (handlers/get-course course-service)}}]

     ;; ============================================
     ;; Course Management (Permission-based)
     ;; ============================================
     ["/admin/courses"
      {:post {:handler (mw/wrap-permission
                        (handlers/create-course course-service)
                        ["manage_courses"])}}]

     ["/admin/courses/:id"
      {:put {:handler (mw/wrap-permission
                       (handlers/update-course course-service)
                       ["manage_courses"])}
       :delete {:handler (mw/wrap-permission
                          (handlers/delete-course course-service)
                          ["manage_courses"])}}]]))
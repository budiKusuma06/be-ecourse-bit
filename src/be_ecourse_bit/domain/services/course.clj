(ns be-ecourse-bit.domain.services.course
  (:require [be-ecourse-bit.domain.repositories.course :as course-repo]))

(defprotocol CourseService
  (list-courses [this])
  (get-course [this id])
  (create-course [this course-data])
  (update-course [this id course-data])
  (delete-course [this id]))

(defrecord DefaultCourseService [course-repository]
  CourseService
  (list-courses [_]
    (course-repo/find-all course-repository))

  (get-course [_ id]
    (course-repo/find-by-id course-repository id))

  (create-course [_ course-data]
    (let [result (course-repo/create! course-repository course-data)]
      (assoc course-data :id (:id result))))

  (update-course [_ id course-data]
    (course-repo/update! course-repository id course-data)
    (assoc course-data :id id))

  (delete-course [_ id]
    (course-repo/delete! course-repository id)))

(defn create-service [course-repository]
  (->DefaultCourseService course-repository))
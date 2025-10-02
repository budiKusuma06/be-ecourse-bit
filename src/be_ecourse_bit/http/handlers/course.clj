(ns be-ecourse-bit.http.handlers.course
  (:require [be-ecourse-bit.domain.services.course :as course-svc]
            [be-ecourse-bit.domain.validators :as v]
            [malli.error :as me]))

(defn- normalize-price [course]
  (if (number? (:price course))
    (assoc course :price (double (:price course)))
    course))

(defn- convert-bigdecimals [course]
  (if (instance? java.math.BigDecimal (:price course))
    (assoc course :price (double (:price course)))
    course))

(defn list-courses [course-service]
  (fn [_]
    {:status 200
     :body (->> (course-svc/list-courses course-service)
                (map convert-bigdecimals))}))

(defn get-course [course-service]
  (fn [request]
    (let [id (Integer/parseInt (-> request :path-params :id))]
      (if-let [course (course-svc/get-course course-service id)]
        {:status 200 :body (convert-bigdecimals course)}
        {:status 404 :body {:error "Course not found"}}))))

(defn create-course [course-service]
  (fn [request]
    (let [input (normalize-price (:body-params request))
          error (v/validate v/course-schema input)]
      (if error
        {:status 400 :body {:error (me/humanize error)}}
        (let [course (course-svc/create-course course-service input)]
          {:status 201 :body (convert-bigdecimals course)})))))

(defn update-course [course-service]
  (fn [request]
    (let [id (Integer/parseInt (-> request :path-params :id))
          input (normalize-price (:body-params request))
          error (v/validate v/course-schema input)]
      (if error
        {:status 400 :body {:error (me/humanize error)}}
        (if (course-svc/get-course course-service id)
          {:status 200
           :body (convert-bigdecimals
                  (course-svc/update-course course-service id input))}
          {:status 404 :body {:error "Course not found"}})))))

(defn delete-course [course-service]
  (fn [request]
    (let [id (Integer/parseInt (-> request :path-params :id))]
      (if (course-svc/delete-course course-service id)
        {:status 204}
        {:status 404 :body {:error "Course not found"}}))))
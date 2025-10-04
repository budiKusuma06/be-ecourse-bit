(ns be-ecourse-bit.http.handlers.admin
  (:require [be-ecourse-bit.domain.services.admin :as admin-svc]
            [be-ecourse-bit.domain.validators :as v]
            [be-ecourse-bit.http.middleware :as mw]
            [malli.error :as me]))

;; ============================================
;; Admin Management
;; ============================================

(defn list-admins [admin-service]
  (fn [_]
    {:status 200
     :body (admin-svc/list-all-admins admin-service)}))

(defn get-admin [admin-service]
  (fn [request]
    (let [admin-id (Integer/parseInt (-> request :path-params :id))]
      (if-let [admin (admin-svc/get-admin-detail admin-service admin-id)]
        {:status 200 :body admin}
        {:status 404 :body {:error "Admin not found"}}))))

(defn update-admin-permissions [admin-service]
  (fn [request]
    (let [admin-id (Integer/parseInt (-> request :path-params :id))
          session-user (mw/get-session-user request)
          input (:body-params request)
          error (v/validate v/admin-permissions-update-schema input)]
      (if error
        {:status 400 :body {:error (me/humanize error)}}
        (try
          (let [result (admin-svc/update-admin-permissions admin-service
                                                           admin-id
                                                           (:permission_ids input)
                                                           (:id session-user))]
            {:status 200 :body result})
          (catch clojure.lang.ExceptionInfo e
            (let [data (ex-data e)]
              (if (= :forbidden (:type data))
                {:status 403 :body {:error (.getMessage e)}}
                {:status 500 :body {:error "Failed to update permissions"}})))
          (catch Exception e
            {:status 500 :body {:error "Failed to update permissions"}}))))))

(defn deactivate-admin [admin-service]
  (fn [request]
    (let [admin-id (Integer/parseInt (-> request :path-params :id))
          session-user (mw/get-session-user request)]
      (try
        (let [result (admin-svc/deactivate-admin admin-service admin-id (:id session-user))]
          {:status 200 :body result})
        (catch clojure.lang.ExceptionInfo e
          (let [data (ex-data e)]
            (if (= :forbidden (:type data))
              {:status 403 :body {:error (.getMessage e)}}
              {:status 500 :body {:error "Failed to deactivate admin"}})))
        (catch Exception e
          {:status 500 :body {:error "Failed to deactivate admin"}})))))

;; ============================================
;; Permissions Management
;; ============================================

(defn list-permissions [admin-service]
  (fn [_]
    {:status 200
     :body (admin-svc/list-all-permissions admin-service)}))

(defn get-admin-permissions [admin-service]
  (fn [request]
    (let [admin-id (Integer/parseInt (-> request :path-params :id))]
      {:status 200
       :body (admin-svc/get-admin-permissions admin-service admin-id)})))
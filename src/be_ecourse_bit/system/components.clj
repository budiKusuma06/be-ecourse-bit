(ns be-ecourse-bit.system.components
  (:require [mount.core :refer [defstate]]
            [be-ecourse-bit.system.config :as config]
            [be-ecourse-bit.infrastructure.database :as db]
            [be-ecourse-bit.infrastructure.redis :as redis]
            [be-ecourse-bit.infrastructure.memory :as memory]
            [be-ecourse-bit.domain.repositories.course :as course-repo]
            [be-ecourse-bit.domain.repositories.user :as user-repo]
            [be-ecourse-bit.domain.repositories.profile :as profile-repo]
            [be-ecourse-bit.domain.repositories.permission :as perm-repo]
            [be-ecourse-bit.domain.services.auth :as auth-svc]
            [be-ecourse-bit.domain.services.course :as course-svc]
            [be-ecourse-bit.domain.services.admin :as admin-svc]
            [be-ecourse-bit.http.router :as router]
            [be-ecourse-bit.http.middleware :as mw]
            [reitit.ring :as reitit]
            [org.httpkit.server :as http]))

;; Config
(defstate config
  :start (config/load-config))

;; Infrastructure - Database
(defstate database
  :start (let [db (db/setup-database (:database config))]
           (db/create-tables! db)
           (db/seed-data! db)
           db)
  :stop (println "→ Database connection closed"))

;; Infrastructure - Session Store
(defstate session-store
  :start (let [redis-config (:redis config)]
           (if (redis/test-connection redis-config)
             (do
               (println "→ Using Redis for session storage")
               (redis/create-redis-store redis-config))
             (do
               (println "→ Using in-memory session storage")
               (memory/create-memory-store)))))

;; Domain - Repositories
(defstate course-repository
  :start (course-repo/create-repository database))

(defstate user-repository
  :start (user-repo/create-repository database))

(defstate profile-repository
  :start (profile-repo/create-repository database))

(defstate permission-repository
  :start (perm-repo/create-repository database))

;; Domain - Services
(defstate auth-service
  :start (auth-svc/create-service user-repository profile-repository permission-repository))

(defstate course-service
  :start (course-svc/create-service course-repository))

(defstate admin-service
  :start (admin-svc/create-service user-repository profile-repository permission-repository))

;; HTTP - Server
(defstate app
  :start (let [app-router (router/create-router {:auth-service auth-service
                                                 :course-service course-service
                                                 :admin-service admin-service
                                                 :user-repository user-repository
                                                 :profile-repository profile-repository
                                                 :session-store session-store})]
           (-> (reitit/ring-handler
                app-router
                (reitit/create-default-handler
                 {:not-found router/not-found-handler
                  :method-not-allowed router/method-not-allowed-handler
                  :not-acceptable (fn [_]
                                    {:status 406
                                     :headers {"Content-Type" "application/json"}
                                     :body {:error "Not acceptable"}})}))
               (mw/wrap-session session-store))))

(defstate http-server
  :start (do
           (println "→ Starting HTTP server on port" (:port (:server config)))
           (http/run-server #'app {:port (:port (:server config))}))
  :stop (do
          (println "→ Stopping HTTP server...")
          (try
            (when http-server
              (http-server :timeout 100))
            (catch Exception e
              (println "  Warning during shutdown:" (.getMessage e))))
          (Thread/sleep 500)
          (println "→ HTTP server stopped")))
(ns be-ecourse-bit.infrastructure.redis
  (:require [taoensso.carmine :as car]
            [ring.middleware.session.store :as ring-store]))

(defrecord RedisStore [config]
  ;; Hanya implement Ring's SessionStore protocol
  ring-store/SessionStore
  (read-session [_ key]
    (when key
      (try
        (let [data (car/wcar config (car/get (str "session:" key)))]
          (when data (read-string data)))
        (catch Exception e
          (println "Error reading session:" (.getMessage e))
          nil))))

  (write-session [_ key data]
    (let [session-key (or key (str (java.util.UUID/randomUUID)))
          redis-key (str "session:" session-key)]
      (try
        (car/wcar config (car/setex redis-key 86400 (pr-str data)))
        session-key
        (catch Exception e
          (println "Error writing session:" (.getMessage e))
          session-key))))

  (delete-session [_ key]
    (when key
      (try
        (car/wcar config (car/del (str "session:" key)))
        nil
        (catch Exception e
          (println "Error deleting session:" (.getMessage e))
          nil)))))

;; Helper functions untuk operasi tambahan (bukan protocol methods)
(defn count-sessions
  "Count active sessions in Redis"
  [^RedisStore store]
  (try
    (let [config (:config store)]
      (count (car/wcar config (car/keys "session:*"))))
    (catch Exception e
      (println "Error counting sessions:" (.getMessage e))
      0)))

(defn list-sessions
  "List all session keys in Redis"
  [^RedisStore store]
  (try
    (let [config (:config store)]
      (car/wcar config (car/keys "session:*")))
    (catch Exception e
      (println "Error listing sessions:" (.getMessage e))
      [])))

(defn create-redis-store
  "Create Redis session store"
  [config]
  (->RedisStore config))

(defn test-connection
  "Test Redis connection"
  [config]
  (try
    (car/wcar config (car/ping))
    true
    (catch Exception e false)))
(ns be-ecourse-bit.infrastructure.memory
  (:require [ring.middleware.session.memory :as memory]
            [ring.middleware.session.store :as ring-store]))

(defrecord MemoryStore [internal-store]
  ;; Hanya implement Ring's SessionStore protocol
  ring-store/SessionStore
  (read-session [_ key]
    (ring-store/read-session internal-store key))

  (write-session [_ key data]
    (ring-store/write-session internal-store key data))

  (delete-session [_ key]
    (ring-store/delete-session internal-store key)))

;; Helper functions untuk operasi tambahan (bukan protocol methods)
(defn count-sessions
  "Count active sessions in memory"
  [^MemoryStore store]
  (count @(.sessions (:internal-store store))))

(defn list-sessions
  "List all session keys in memory"
  [^MemoryStore store]
  (keys @(.sessions (:internal-store store))))

(defn create-memory-store
  "Create in-memory session store"
  []
  (->MemoryStore (memory/memory-store)))
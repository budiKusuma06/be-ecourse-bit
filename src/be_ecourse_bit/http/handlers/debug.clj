(ns be-ecourse-bit.http.handlers.debug
  (:require [be-ecourse-bit.domain.repositories.user :as user-repo]
            [be-ecourse-bit.infrastructure.redis :as redis]
            [be-ecourse-bit.infrastructure.memory :as memory]))

(defn list-users [user-repository]
  "List all users (without password hash)"
  (fn [_]
    {:status 200
     :body (user-repo/find-all user-repository)}))

(defn session-stats [session-store]
  "Get session statistics"
  (fn [_]
    (let [count (cond
                  (instance? be_ecourse_bit.infrastructure.redis.RedisStore session-store)
                  (redis/count-sessions session-store)

                  (instance? be_ecourse_bit.infrastructure.memory.MemoryStore session-store)
                  (memory/count-sessions session-store)

                  :else 0)

          keys (cond
                 (instance? be_ecourse_bit.infrastructure.redis.RedisStore session-store)
                 (take 10 (redis/list-sessions session-store))

                 (instance? be_ecourse_bit.infrastructure.memory.MemoryStore session-store)
                 (take 10 (memory/list-sessions session-store))

                 :else [])]

      {:status 200
       :body {:active-sessions count
              :session-keys keys}})))
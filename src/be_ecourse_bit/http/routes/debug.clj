(ns be-ecourse-bit.http.routes.debug
  (:require [be-ecourse-bit.http.handlers.debug :as handlers]
            [be-ecourse-bit.http.middleware :as mw]))

(defn routes [services]
  (let [{:keys [user-repository session-store]} services]
    [["/debug/users"
      {:get {:handler (handlers/list-users user-repository)}}]

     ["/debug/sessions"
      {:get {:handler (mw/wrap-auth
                       (handlers/session-stats session-store)
                       {:roles #{"admin"}})}}]]))
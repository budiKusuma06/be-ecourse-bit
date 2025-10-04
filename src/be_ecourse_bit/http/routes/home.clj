(ns be-ecourse-bit.http.routes.home
  (:require [be-ecourse-bit.http.handlers.home :as handlers]))

(defn routes [_services]
  [["/"
    {:get {:handler (handlers/home)}}]

   ["/health"
    {:get {:handler (handlers/health)}}]])
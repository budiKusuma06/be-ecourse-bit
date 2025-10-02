(ns be-ecourse-bit.infrastructure.session-store
  (:require [ring.middleware.session.store :as ring-store]))

(defprotocol SessionStore
  "Protocol for session storage operations"
  (read-session [this key])
  (write-session [this key data])
  (delete-session [this key])
  (count-sessions [this])
  (list-sessions [this]))
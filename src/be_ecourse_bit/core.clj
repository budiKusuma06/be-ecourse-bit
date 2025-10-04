(ns be-ecourse-bit.core
  (:require [mount.core :as mount]
            [be-ecourse-bit.system.components])
  (:gen-class))

(defn -main [& args]
  (println "========================================")
  (println "e-Course API - Plugin Architecture")
  (println "========================================")
  (mount/start)
  (println "========================================")
  (println "Server is running")
  (println "→ Access API at: http://localhost:3000")
  (println "========================================")
  @(promise))
(ns user
  (:require [mount.core :as mount]
            [clj-reload.core :as reload]
            [hawk.core :as hawk]
            [be-ecourse-bit.system.components]))

;; ============================================
;; Manual Control Functions (for REPL)
;; ============================================

(defn start
  "Start the application"
  []
  (println "\n🚀 Starting e-Course API...")
  (mount/start)
  (println "✅ Server running at http://localhost:3000\n"))

(defn stop
  "Stop the application"
  []
  (println "\n🛑 Stopping e-Course API...")
  (mount/stop)
  (println "✅ Server stopped\n"))

(defn restart
  "Restart the application"
  []
  (println "\n🔄 Restarting e-Course API...")
  (stop)
  (start)
  (println "✅ Server restarted\n"))

(defn reload-and-restart
  "Reload changed namespaces and restart the server"
  []
  (println "\n🔄 Reloading code and restarting...")
  (stop)
  (Thread/sleep 1000)
  (reload/reload)
  (start)
  (println "✅ Code reloaded and server restarted\n"))

;; ============================================
;; Auto-reload Configuration
;; ============================================

(def ^:private watcher (atom nil))

(defn- reload-handler [_ {:keys [file]}]
  (when (and file (.endsWith (.getName file) ".clj"))
    (println "\n📝 File changed:" (.getPath file))
    (reload-and-restart)))

(defn start-auto-reload
  "Start watching for file changes and auto-reload"
  []
  (when @watcher
    (hawk/stop! @watcher))

  (println "\n👁️  Starting auto-reload watcher...")
  (reset! watcher
          (hawk/watch! [{:paths ["src" "dev"]
                         :filter hawk/file?
                         :handler reload-handler}]))
  (println "✅ Auto-reload enabled for src/ and dev/ directories")
  (println "   Changes to .clj files will trigger automatic reload\n"))

(defn stop-auto-reload
  "Stop watching for file changes"
  []
  (when @watcher
    (println "\n🛑 Stopping auto-reload watcher...")
    (hawk/stop! @watcher)
    (reset! watcher nil)
    (println "✅ Auto-reload disabled\n")))

;; ============================================
;; Development Helpers
;; ============================================

(defn status
  "Show current server status"
  []
  (println "\n📊 Server Status:")
  (println "  Mount states:" (if (mount/running-states) "Running" "Stopped"))
  (println "  Auto-reload:" (if @watcher "Enabled" "Disabled"))
  (when (mount/running-states)
    (println "\n  Running components:")
    (doseq [state (mount/running-states)]
      (println "   -" state)))
  (println))

(defn dev-mode
  "Start server with auto-reload enabled"
  []
  (start)
  (start-auto-reload))

(defn prod-mode
  "Start server without auto-reload"
  []
  (stop-auto-reload)
  (start))

;; ============================================
;; Quick Commands
;; ============================================

(comment
  ;; Basic commands
  (start)                    ; Start server
  (stop)                     ; Stop server
  (restart)                  ; Restart server
  (reload-and-restart)       ; Reload code and restart

  ;; Auto-reload
  (start-auto-reload)        ; Enable auto-reload
  (stop-auto-reload)         ; Disable auto-reload
  (dev-mode)                 ; Start with auto-reload

  ;; Status
  (status)                   ; Check server status

  ;; Mount states
  (mount/running-states)     ; List running states
  (mount/start #'be-ecourse-bit.system.components/http-server) ; Start specific state
  (mount/stop #'be-ecourse-bit.system.components/http-server)  ; Stop specific state
  )
(ns be-ecourse-bit.system.config)

(defn load-config []
  {:database {:dbtype "mysql"
              :host "localhost"
              :port 3306
              :dbname "ecourse_db"
              :user "root"
              :password "root"
              :allowPublicKeyRetrieval true
              :useSSL false
              :serverTimezone "UTC"
              :nullCatalogMeansCurrent false}

   :redis {:pool {}
           :spec {:host (or (System/getenv "REDIS_HOST") "localhost")
                  :port (Integer/parseInt (or (System/getenv "REDIS_PORT") "6379"))
                  :password (System/getenv "REDIS_PASSWORD")}}

   :server {:port 3000}})
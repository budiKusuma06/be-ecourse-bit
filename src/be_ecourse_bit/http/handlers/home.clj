(ns be-ecourse-bit.http.handlers.home)

(defn home []
  (fn [_]
    {:status 200
     :body {:message "Hello World"}}))

(defn health []
  (fn [_]
    {:status 200
     :body {:status "OK"
            :timestamp (System/currentTimeMillis)}}))
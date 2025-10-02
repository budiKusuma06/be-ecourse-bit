(defproject be-ecourse-bit "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.11.1"]
                 ;; Lifecycle
                 [mount "0.1.23"]
                 ;; Web Server
                 [ring/ring-core "1.15.3"]
                 [http-kit "2.8.1"]
                 [metosin/reitit "0.9.1"] 
                 [metosin/muuntaja "0.6.11"]
                 [metosin/malli "0.19.1"]
                 [metosin/jsonista "0.3.13"]
                 ;; Redis
                 [com.taoensso/carmine "3.4.1"]
                 ;; Database
                 [com.github.seancorfield/next.jdbc "1.3.1070"]
                 [com.mysql/mysql-connector-j "9.4.0"]
                 ;; Auth
                 [buddy/buddy-hashers "2.0.167"]
                 ;; Utilities
                 [clj-time "0.15.2"]]
  :main ^:skip-aot be-ecourse-bit.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all
                       :jvm-opts ["-Dclojure.compiler.direct-linking=true"]}})

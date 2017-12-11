(defproject dojo-appointment "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.9.0"]
                 [org.clojure/data.xml "0.0.8"]
                 [liberator "0.14.1"]
                 [metosin/compojure-api "2.0.0-alpha16"]
                 [metosin/spec-tools "0.5.1"]
                 [http-kit "2.1.18"]
                 [ring/ring-core "1.6.3"]]
  :plugins [[lein-ring "0.12.2"]]
  :ring {:handler dojo-appointment.core/handler})

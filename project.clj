(defproject todo-ddom "0.0.1-SNAPSHOT"
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/clojurescript "1.7.228"]
                 [ewen/hiccup "1.0.0"]
                 [ewen/ddom "0.0.1-SNAPSHOT"]
                 [io.pedestal/pedestal.service "0.4.2-SNAPSHOT"]
                 [io.pedestal/pedestal.immutant "0.4.2-SNAPSHOT"]
                 [org.slf4j/jul-to-slf4j "1.7.12"]
                 [ch.qos.logback/logback-core "1.1.2"
                  :exclusions [org.slf4j/slf4j-api]]]
  :plugins [[lein-cljsbuild "1.1.3"]]
  :cljsbuild {:builds
              [{:source-paths ["src"]
                :compiler {:output-to "resources/public/javascript/main.js"
                           :optimizations :advanced}}]}
  :aot [todo-ddom.ReplAppender])

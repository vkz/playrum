(defproject play "0.1.0-SNAPSHOT"
  :description ""
  :url ""
  :license {:name "The MIT License"
            :url "https://opensource.org/licenses/MIT"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/clojurescript "1.9.293"]
                 [hiccup "1.0.5"]]
  :profiles {:dev {:plugins [[lein-cljsbuild "1.1.5"]
                             [lein-figwheel "0.5.9"]]
                   ;; https://github.com/bhauman/lein-figwheel/wiki/Using-the-Figwheel-REPL-within-NRepl#integration-with-emacscider
                   :dependencies [[com.cemerick/piggieback "0.2.1"]
                                  [figwheel-sidecar "0.5.9"]]
                   :source-paths ["dev"]}}
  :cljsbuild {:builds [{:id "dev"
                        :source-paths ["src" "dev"]
                        :figwheel true
                        :compiler
                        {:main "play.client"
                         :output-to "target/app.js"
                         :output-dir "target/dev"
                         :asset-path "target/dev"
                         :optimizations :none
                         :source-map true
                         :compiler-stats true
                         :parallel-build true
                         :recompile-dependents true}}
                       {:id "advanced"
                        :source-paths ["src"]
                        :compiler
                        {:output-to "target/app.js"
                         :optimizations :advanced
                         :source-map "target/app.js.map"
                         :pretty-print false
                         :compiler-stats true
                         :parallel-build true}}]})




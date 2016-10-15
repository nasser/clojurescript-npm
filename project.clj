(defproject clojurescript-npm "0.2.0-SNAPSHOT"
  :description "The ClojureScript Programming Language, packaged for Node.js"
  :url "https://github.com/nasser/clojurescript-npm"
  :license {:name "Eclipse Public License"
            :url "http://nasser.github.io/clojurescript-npm/blob/master/LICENSE"}
  :dependencies [[org.clojure/clojure "1.9.0-alpha1"]
                 [org.clojure/clojurescript "1.9.14"]
                 [replumb/replumb "0.2.1"]]
  :clean-targets [:target-path "out"]
  :plugins [[lein-cljsbuild "1.1.1"]]
  :cljsbuild {
    :builds [{
        :source-paths ["src"]
        :compiler {
          :main          clojurescript.core
          :output-to     "lib/bootstrap.js"
          :output-dir    "out"
          :target        :nodejs
          :hashbang      false
          :optimizations :simple}}]})

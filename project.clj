(defproject dtorter "0.1.0-SNAPSHOT"
  :dependencies [[org.clojure/clojure "1.10.1"]
                 [net.mikera/core.matrix "0.62.0"]
                 [net.mikera/vectorz-clj "0.48.0"]

                 [io.pedestal/pedestal.service "0.5.5"]
                 [io.pedestal/pedestal.jetty "0.5.5"]
                 [metosin/reitit-pedestal "0.5.18"]
                 [metosin/reitit "0.5.18"]

                 [com.xtdb/xtdb-core "1.20.0"]
                 [com.xtdb/xtdb-lmdb "1.20.0"]
                 
                 [cheshire "5.10.2"]
                 [hiccup "1.0.5"]
                 
                 [cryptohash-clj "0.1.10"]

                 [criterium "0.4.6"]

                 [integrant "0.8.0"]

                 [babashka/fs "0.1.6"]
                 [kixi/stats "0.5.0"]
                 
                 [com.github.oliyh/martian "0.1.21"]
                 [com.github.oliyh/martian-clj-http "0.1.21"]
                 
                 [org.clojure/data.priority-map "1.1.0"]
                 
                 [org.slf4j/slf4j-api "1.7.30"]
                 [org.slf4j/jul-to-slf4j "1.7.30"]
                 [org.slf4j/jcl-over-slf4j "1.7.30"]
                 [org.slf4j/log4j-over-slf4j "1.7.30"]
                 [org.slf4j/osgi-over-slf4j "1.7.30"]
                 [ch.qos.logback/logback-classic "1.2.3"]
                 [expound "0.9.0"]

                 [dom-top/dom-top "1.0.7"]
                 
                 [vvvvalvalval/scope-capture "0.3.3"]]
  :injections [(require 'sc.api)]
  :repl-options {:init-ns dev-resources.user}
  :repositories [["sonatype snapshots" {:url "https://s01.oss.sonatype.org/content/repositories/snapshots"}]]
  :source-paths ["src"]
  
  :main dtorter.core
  :aot [dtorter.core])

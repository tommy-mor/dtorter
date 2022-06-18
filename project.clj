(defproject dtorter "0.1.0-SNAPSHOT"
  :dependencies [[org.clojure/clojure "1.10.1"]
                 [cheshire "5.10.2"]
                 
                 [net.mikera/core.matrix "0.62.0"]
                 [net.mikera/vectorz-clj "0.48.0"]

                 [com.xtdb/xtdb-core "1.20.0"]
                 [com.xtdb/xtdb-lmdb "1.20.0"]
                 
                 [hiccup "1.0.5"]
                 [cryptohash-clj "0.1.10"]

                 [criterium "0.4.6"]

                 [babashka/fs "0.1.6"]
                 [kixi/stats "0.5.0"]
                 
                 [org.clojure/tools.namespace "1.3.0"]

                 
                 [org.clojure/data.priority-map "1.1.0"]
                 [yada "1.4.0-alpha1"]
                 [juxt/clip "0.27.0"]]
  
  
  
  :repl-options {:init-ns dev-resources.user}
  :repositories [["sonatype snapshots" {:url "https://s01.oss.sonatype.org/content/repositories/snapshots"}]]
  :source-paths ["src"]
  
  :main dtorter.core
  :aot [dtorter.core])

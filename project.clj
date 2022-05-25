(defproject dtorter "0.1.0-SNAPSHOT"
  :dependencies [[org.clojure/clojure "1.10.1"]
                 [cheshire "5.10.2"]
                 
                 [net.mikera/core.matrix "0.62.0"]
                 [net.mikera/vectorz-clj "0.48.0"]

                 [io.pedestal/pedestal.service "0.5.5"]
                 [io.pedestal/pedestal.jetty "0.5.5"]

                 [com.walmartlabs/lacinia "1.0"]
                 [com.walmartlabs/lacinia-pedestal "1.0"]
                 
                 [com.xtdb/xtdb-core "1.20.0"]
                 [com.xtdb/xtdb-lmdb "1.20.0"]
                 
                 [hiccup "1.0.5"]
                 [cryptohash-clj "0.1.10"]

                 [re-graph "0.1.17"]
                 [criterium "0.4.6"]

                 [integrant "0.8.0"]

                 [babashka/fs "0.1.6"]]
  
  
  
  :repl-options {:init-ns dev-resources.user}
  :repositories [["sonatype snapshots" {:url "https://s01.oss.sonatype.org/content/repositories/snapshots"}]])

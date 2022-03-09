(defproject dtorter "0.1.0-SNAPSHOT"
  :dependencies [[org.clojure/clojure "1.10.1"]
                 [com.datomic/client-pro "1.0.72"]
                 [cheshire "5.10.2"]
                 [net.mikera/core.matrix "0.62.0"]
                 [net.mikera/vectorz-clj "0.48.0"]]
  
  :repositories {"my.datomic.com" {:url "https://my.datomic.com/repo"
                                   :creds :gpg}}
  :repl-options {:init-ns dtorter.data})

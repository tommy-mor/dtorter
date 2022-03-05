(defproject dtorter "0.1.0-SNAPSHOT"
  :dependencies [[org.clojure/clojure "1.10.1"]
                 [com.datomic/datomic-pro "1.0.6362"]]
  
  :repositories {"my.datomic.com" {:url "https://my.datomic.com/repo"
                                   :creds :gpg}}
  :repl-options {:init-ns dtorter.core})

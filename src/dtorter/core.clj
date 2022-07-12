(ns dtorter.core
  (:require [dtorter.http :as http]
            [nrepl.server :as nrepl-server]
            [cider.nrepl :refer [cider-nrepl-handler]])
  (:gen-class))

(defn -main [& args]
  (println "welcome to my uberjar")
  (nrepl-server/start-server :port 7888 :handlre cider-nrepl-handler)
  (http/start {:prod true}))

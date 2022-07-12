(ns dtorter.core
  (:require [dtorter.http :as http]
            [nrepl.server :as nrepl-server]
            [cider.nrepl :refer [cider-nrepl-handler]])
  (:gen-class))

(defn -main [& args]
  (println "welcome to my uberjar")
  ;; https://markusgraf.net/2020-03-03-Remote-Cider-nREPL.html
  ;;  ssh -L 7889:localhost:8081 root@sorter.isnt.online
  (nrepl-server/start-server :port 7888 :handler cider-nrepl-handler)
  (http/start {:prod true}))

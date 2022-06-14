(ns dtorter.core
  (:require [dtorter.http :as http])
  (:gen-class))

(defn -main [& args]
  (println "welcome to my uberjar")
  (http/start {:prod true}))


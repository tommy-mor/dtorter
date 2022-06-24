(ns dtorter.views.routes
  (:require [dtorter.views.front-page :as fp]))

(defn routes [] 
  [["/epic"
    {:get {:handler (fn [req]
                      (def req req)
                      (println "here")
                      {:status 200 :html (fp/page req)})}}]])

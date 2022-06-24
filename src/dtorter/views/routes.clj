(ns dtorter.views.routes
  (:require [dtorter.views.front-page :as fp]))

(def routes
  [[""
    {:get {:handler
           (fn [req] {:status 200 :html (fp/page req)})}}]])

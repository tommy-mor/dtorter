(ns dtorter.views.front-page)

(defn frame []
  [:h1 "st"])

(defn front-page [req]
  {:status 200
   :html (frame)})

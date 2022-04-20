(ns dtorter.user
  (:require
   [ring.middleware.resource :refer [wrap-resource]]))

(def app (wrap-resource identity "public"))

(defn cljs []
  (shadow/repl :app))

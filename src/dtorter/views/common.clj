(ns dtorter.views.common
  (:require [reitit.core :as r]))

(defn rurl-for
  ([ctx name] (rurl-for ctx name {}))
  ([ctx name args]
   (-> ctx
       :request
       ::r/router
       (r/match-by-name name args)
       :path)))



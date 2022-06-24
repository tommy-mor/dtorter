(ns dtorter.views.common
  (:require [reitit.core :as r]))

(defn rurl-for [ctx name]
  (-> ctx :request
      ::r/router
      (r/match-by-name name)
      :path))



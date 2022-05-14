(ns dtorter.main
  (:require
   [dtorter.data :as data]
   [dtorter.http :as http]
   [dtorter.api :as api]
   [com.stuartsierra.component :as component]))


(defn new-system []
  (merge (component/system-map)
         (http/new-server)
         (api/new-schema-provider)))

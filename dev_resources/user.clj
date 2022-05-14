(ns user
  (:require
   [dtorter.main :as main]
   [com.stuartsierra.component :as component]))

(defonce system (main/new-system))

(defn start [] (alter-var-root #'system component/start-system))

(defn stop [] (alter-var-root #'system component/stop-system))

(ns dev-resources.user
  (:require
   [dtorter.system :as system]
   [com.stuartsierra.component :as component]
   [clojure.tools.namespace.repl :refer [refresh]]))

(def system nil)

(defn init [] (alter-var-root #'system (constantly (system/new-system))))

(defn start [] (alter-var-root #'system component/start-system))

(defn stop [] (alter-var-root #'system component/stop-system))


(defn go []
  (init)
  (start))

(defn reset []
  (stop)
  (refresh :after 'dev-resources.user/go))

(go)
(comment
  (reset))







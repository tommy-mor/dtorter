(ns dev-resources.user
  (:require
   [clojure.tools.namespace.repl :refer [refresh]]
   [juxt.clip.repl :refer [start stop reset set-init! system]]
   [dtorter.http :as http]))

(set-init! #(http/system-config :dev))



(comment
  (start)
  (stop))



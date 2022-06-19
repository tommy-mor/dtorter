(ns dtorter.tag
  (:require [shared.specs :as specs]
            [clojure.spec.alpha :as s]
            [yada.yada :refer [resource]]))


(def tag-resource
  (resource
   {:id :sorter/tag
    :description "collection of items in sorter"
    :properties (fn [ctx] {:exists? true})

    :methods
    {:get {:produces "text/html"
           :response "this is a tag"}
     }}))

(comment (juxt.clip.repl/start)
         (juxt.clip.repl/stop))





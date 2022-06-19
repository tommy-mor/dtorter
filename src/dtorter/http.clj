(ns dtorter.http
  (:require [dtorter.db :as db]
            [dtorter.api :as api]
            [dtorter.data :as data]
            [yada.yada :as yada]
            [juxt.clip.core :as clip]
            [clojure.spec.alpha :as s]
            [dtorter.tag :refer [tag-resource]]))



(defn api-routes []
  [["/tag/" :id] tag-resource])

(defn routes []
  [""
   [["/api" (yada/swaggered (api-routes)
                            {:info {:title "sorter api"
                                    :version "0.1"
                                    :description "for creating sorter things"}
                             :basePath "/api"})]
    [true (yada/yada nil)]]])

(comment (juxt.clip.repl/start)
         (juxt.clip.repl/stop))





;; TODO use :server/enable-session {}














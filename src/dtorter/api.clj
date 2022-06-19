(ns dtorter.api
  (:require [clojure.java.io :as io]
            [clojure.edn  :as edn]
            [clojure.walk :refer [postwalk]]

            [dtorter.queries :as queries]
            [dtorter.mutations :as mutations]
            [dtorter.math :as math]
            [dtorter.util :refer [strip]]
            [xtdb.api :as xt]))


(def api-interceptors
  [])

(def tag-interceptor
  {:enter (fn [ctx]
            (def ctx ctx)
            ctx)})

(def api-routes
  [
   ["/tag/:id"
    {:parameters {:path {:id string?}}
     :interceptors [tag-interceptor]
     
     :get
     {:handler (fn [{:keys [node] :as req}]
                 (def node node)
                 (def req req)
                 ;; try to put as much logic as possible in xtdb..
                 {:status 200 :body (xt/pull (xt/db node) '[*] (-> req :path-params :id))})}}]
   
   ["/item/:id"
    {:parameters {:path {:id string?}}
     :interceptors [tag-interceptor]
     
     :get
     {:handler (fn [{:keys [node] :as req}]
                 {:status 200 :body {:woah 300}})}}]])

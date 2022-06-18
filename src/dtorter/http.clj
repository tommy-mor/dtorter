(ns dtorter.http
  (:require [dtorter.db :as db]
            [dtorter.api :as api]
            [dtorter.data :as data]
            [yada.yada :as yada]
            [juxt.clip.core :as clip]))



(defn hello-routes []
  ["/hello"
   (yada/handler "hello world\n")])

(defn routes []
  [""
   [["/hello" (yada/resource
               {:methods {:get {:produces "text/html"
                                :response "yo"}}})]
    [true (yada/resource
           {:methods {:get {:produces "text/html"
                            :response "not found"}}})]]
   ])

(defn system-config [_]
  {:components
   {:handler {:start `(routes)}
    :http {:start `(yada/listener (clip/ref :handler) {:port 8080})
           :stop '((:close this))
           :resolve :server}}})

(comment (juxt.clip.repl/start)
         (juxt.clip.repl/stop))





;; TODO use :server/enable-session {}














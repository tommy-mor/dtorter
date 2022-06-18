(ns dtorter.http
  (:require [dtorter.db :as db]
            [dtorter.api :as api]
            [dtorter.data :as data]
            [yada.yada :as yada]
            [juxt.clip.core :as clip]))



(defn routes []
  ["/hello"
   (yada/handler "hello world\n")])

(defn system-config [_]
  {:components
   {:handler {:start (yada/handler "test")}
    :http {:start `(yada/listener (clip/ref :handler) {:port 3000})
           :stop '((:close this))
           :resolve :server}}})





;; TODO use :server/enable-session {}














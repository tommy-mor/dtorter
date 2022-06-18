(ns dtorter.http
  (:require [dtorter.db :as db]
            [dtorter.api]
            [yada.yada :as yada]))

(+ 3 3)

(yada/handler
 {:methods
  {:get
   {:produces "text/html"
    :response "<h1>Hello World!</h1>"}}})





;; TODO use :server/enable-session {}














(ns dtorter.exceptions
  (:require [reitit.http.interceptors.exception :as exception]
            [io.pedestal.log :as log]))

;; https://github.com/metosin/reitit/blob/master/modules/reitit-interceptors/src/reitit/http/interceptors/exception.clj

;; copied from
;; https://github.com/metosin/reitit/blob/master/doc/ring/exceptions.md


#_(defn handler [message exception request]
  {:status 500
   :body {:message message
          :exception (.getClass exception)
          :data (ex-data exception)
          :uri (:uri request)}})

(def middleware
  (exception/exception-interceptor
   (merge
    exception/default-handlers
    {::exception/wrap (fn [handler e request]
                        (log/error :error (.printStackTrace e) :request request)
                        (handler e request))})))



(ns dtorter.views.common
  (:require [reitit.core :as r]
            [clojure.string :as str]
            [hiccup.core :refer [html]]))

(defn rurl-for
  ([ctx name] (rurl-for ctx name {}))
  ([ctx name args]
   (-> ctx
       :request
       ::r/router
       (r/match-by-name name args)
       :path)))

(def html-interceptor
  {:name  ::html-response
   :leave (fn [{:keys [response]
                :as   ctx}]
            (if (contains? response :html)
              (let [html-body (->> response
                                   :html
                                   html
                                   (str "\n"))]
                (assoc ctx :response (-> response
                                         (assoc :body html-body)
                                         (assoc-in [:headers "Content-Type"] "text/html"))))
              ctx))})



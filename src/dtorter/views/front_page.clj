(ns dtorter.views.front-page
  (:require [io.pedestal.http.route :refer [url-for]]
            [io.pedestal.http.body-params :as body-params]
            [hiccup.core :refer [html]]
            [cryptohash-clj.api :as c]
            [cryptohash-clj.encode :as enc]
            [xtdb.api :as xt]
            [dtorter.hashing :as hashing]
            [dtorter.queries :as queries]
            [cheshire.core :as json]
            [dtorter.views.tag :as tag]
            [dtorter.views.common :refer [layout]]

            [tdsl.show]))


(defn render-tag [tag]
  [:li
   [:a {:href 3 #_(url-for :tag-page :params {:tagid (:xt/id tag)})} (:tag/name tag)]])


(defn page [request]
  (def request request)
  [:div
   [:h1 "front page"]
   [:ul
    (for [tag [3 4 5]]
      (render-tag tag))]])

;; TODO put these into arguments to init/bang, not here
(defn routes [common-interceptors]
  (into #{["/" :get
           (into common-interceptors [front-page]) :route-name :front-page]
          ["/users" :get
           (into common-interceptors [users-page]) :route-name :users-page]
          ["/login" :get
           (into common-interceptors [login-page]) :route-name :login-page]
          ["/login" :post
           (into common-interceptors [(body-params/body-params) login-done]) :route-name :login-submit]
          ["/logoff" :get
           (into common-interceptors [log-off]) :route-name :log-off]

          ["/t/:tagid" :get
           (into common-interceptors [tag/tag-page]) :route-name :tag-page]
          ["/t/:tagid/:itemid" :get
           (into common-interceptors [tag/item-page]) :route-name :item-page]}
        (tdsl.show/routes common-interceptors)))



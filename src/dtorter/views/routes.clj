(ns dtorter.views.routes
  (:require [dtorter.views.front-page :as fp]
            [clojure.string :as str]
            [dtorter.views.login :as login]
            [dtorter.views.common :as c]
            [dtorter.views.tag :as tag]))

(defn routes []
  [""
   {:interceptors [c/html-interceptor c/layout-interceptor]}
   ["/"
    {:name :front-page
     :get {:handler (fn [req] {:status 200
                               :title "frontpage"
                               :html (fp/page req)})}}]
   ["/t/:id/i/:itemid"
    {:name :item-page
     :parameters {:path {:id string? :itemid string?}}
     :get {:handler tag/item-handler}}]
   ["/t/:id"
    {:name :tag-page
     :parameters {:path {:id string?}}
     :get {:handler tag/tag-handler}}]
   ["/t/:id/graph"
    {:name :graph-page
     :parameters {:path {:id string?}}
     :get {:handler tag/graph-handler}}]

   (login/login-routes)])

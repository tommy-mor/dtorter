(ns dtorter.views.routes
  (:require [dtorter.views.front-page :as fp]
            [clojure.string :as str]
            [dtorter.views.login :as login]
            [dtorter.views.common :as c]))

(def spa-handler {:name :front-page
                  :get {:handler (fn [req] {:status 200
                                            :title "frontpage"
                                            :html (fp/page req)})}})
(defn routes []
  [""
   {:interceptors [c/html-interceptor c/layout-interceptor]}
   ;;TODO make this same exact router as cljs one
   ["/" spa-handler]
   ["/t/*" (assoc spa-handler :name :tag-page)]
   ["/i/*" (assoc spa-handler :name :item-page)]
   ["/intg/youtube" (assoc spa-handler :name :yt-page)]
   ["/change_theme" {:post {:handler c/change-theme-handler}}]
   (login/login-routes)

   (comment ["/t/:id/i/:itemid"
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
              :get {:handler tag/graph-handler}}])])

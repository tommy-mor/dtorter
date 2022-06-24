(ns dtorter.views.routes
  (:require [dtorter.views.front-page :as fp]
            [clojure.string :as str]
            [hiccup.core :refer [html]]
            [dtorter.views.login :as login]
            [dtorter.views.common :as c]))

(defn layout [{:keys [request response] :as ctx} inner]
  (let [title (:title response)
        session (-> request :session)]
    
    [:html
     [:head
      [:meta {:charset "utf-8"}]
      [:meta {:name "viewport"
              :content "width=device-width, initial-scale=1.0"}]
      [:link {:href "/css/site.css"
              :rel "stylesheet"
              :type "text/css"}]
      [:script {:src "/js/shared.js"
                :type "text/javascript"}]
      [:title (or (str title ", sorter") "sorter")]]
     [:div.topbar
      [:span (prn-str session)]
      [:div.topleft
       [:span "sorter"]
       [:a.currentpage {:href (c/rurl-for ctx :front-page)} "home"]
       [:a.currentpage {:href 3 #_(url-for :users-page)} "users"]]
      (if-let [username (:user-name session)]
        [:div.topright
         [:a.currentpage {:href (c/rurl-for ctx :logff)} "logoff"]]
        [:div.topright
         [:a.currentpage {:href (c/rurl-for ctx :login)} "login"]
         [:a.currentpage "make account"]])
      [:div.mainbody
       inner]]]))

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

(def layout-interceptor
  {:leave (fn [{:keys [response]
                :as ctx}]
            (if (contains? response :html)
              (assoc-in ctx [:response :html] (layout ctx (:html response)))
              ctx))})

(defn routes []
  [""
   {:interceptors [html-interceptor layout-interceptor]}
   ["/"
    {:name :front-page
     :get {:handler (fn [req] {:status 200
                               :title "frontpage"
                               :html (fp/page req)})}}]
   (login/login-routes)])

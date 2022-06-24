(ns dtorter.views.routes
  (:require [dtorter.views.front-page :as fp]
            [clojure.string :as str]
            [reitit.core :as r]
            [hiccup.core :refer [html]]))

(defn layout [{:keys [session]} inner {:keys [title]}]
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
     [:a.currentpage {:href 3 #_(url-for :front-page)} "home"]
     [:a.currentpage {:href 3 #_(url-for :users-page)} "users"]]
    (if-let [username (:user-name session)]
      [:div.topright
       [:a.currentpage {:href 3 #_(url-for :log-off)} "logoff"]]
      [:div.topright
       [:a.currentpage {:href 3 #_(url-for :login-page)} "login"]
       [:a.currentpage "make account"]])
    [:div.mainbody
     inner]]])

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
              (assoc-in ctx [:response :html] (layout ctx (:html response) {:title "woah"}))
              ctx))})

(defn routes [] 
  [["/"
    {:interceptors [html-interceptor layout-interceptor]
     :get {:handler (fn [req] {:status 200 :html (fp/page req)})}}]])

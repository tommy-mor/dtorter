(ns dtorter.views.common
  (:require [io.pedestal.http.route :refer [url-for]]))

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
     [:a.currentpage {:href (url-for :front-page)} "home"]
     [:a.currentpage {:href (url-for :users-page)} "users"]]
    (if-let [username (:user-name session)]
      [:div.topright
       [:a.currentpage {:href (url-for :log-off)} "logoff"]]
      [:div.topright
       [:a.currentpage {:href (url-for :login-page)} "login"]
       [:a.currentpage "make account"]])
    [:div.mainbody
     inner]]])

(ns dtorter.views.routes
  (:require [dtorter.views.front-page :as fp]
            [clojure.string :as str]
            [dtorter.views.login :as login]
            [dtorter.views.common :as c]
            [dtorter.views.tag :as tag]))

(defn layout [{:keys [request response] :as ctx} inner]
  (let [title (:title response)
        session (-> request :session)]
    
    (def ctx ctx)
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
      [:div.topleft
       [:span "sorter"]
       [:a.currentpage {:href (c/rurl-for ctx :front-page)} "home"]
       [:a.currentpage {:href 3 #_(url-for :users-page)} "users"]]
      (if-let [username (:user-name session)]
        [:div.topright
         [:a.currentpage {:href (c/rurl-for ctx :logoff)} "logoff"]
         [:span (:user-name session)]]
        [:div.topright
         [:a.currentpage {:href (c/rurl-for ctx :login)} "login"]
         [:a.currentpage "make account"]
         [:span (prn-str session)]])
      [:div.mainbody
       inner]]]))



(def layout-interceptor
  {:leave (fn [{:keys [response]
                :as ctx}]
            (if (contains? response :html)
              (assoc-in ctx [:response :html] (layout ctx (:html response)))
              ctx))
   :enter (fn [ctx]
            (assoc-in ctx [:request :href-for] (partial c/rurl-for ctx)))})

(defn routes []
  [""
   {:interceptors [c/html-interceptor layout-interceptor]}
   ["/"
    {:name :front-page
     :get {:handler (fn [req] {:status 200
                               :title "frontpage"
                               :html (fp/page req)})}}]
   ["/t/:id"
    {:name :tag-page
     :parameters {:path {:id string?}}
     :get {:handler
           tag/tag-handler}}]
   
   (login/login-routes)])

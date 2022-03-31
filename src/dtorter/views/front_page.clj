(ns dtorter.views.front-page)

(defn layout [inner]
  [:html
   [:head
    [:meta {:charset "utf-8"}]
    [:meta {:name "viewport"
            :content "width=device-width; initial-scale=1.0"}]
    [:link {:href "/css/site.css"
            :rel "stylesheet"
            :type "text/css"}]
    [:title "sorter"]]
   [:div.topbar
    [:div.topleft
     [:span "sorter"]
     [:a.currentpage "home"]
     [:a.currentpage "users"]]
    [:div.topright
     [:a.currentpage "home"]
     [:a.currentpage "users"]
     [:a.currentpage "login"]
     [:a.currentpage "make account"]]
    [:div.mainbody
     inner]]])

(defn front-page [req]
  {:status 200
   :html (layout [:h1 "inner"])})

(ns dtorter.views.front-page
  (:require [io.pedestal.http.route :refer [url-for]]
            [hiccup.core :refer [html]]))

(def html-response
  "If the response contains a key :html,
     it take the value of these key,
     turns into HTML via hiccup,
     assoc this HTML in the body
     and set the Content-Type of the response to text/html"
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
     [:a.currentpage {:href (url-for :front-page)} "home"]
     [:a.currentpage {:href (url-for :users-page)} "users"]]
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

(defn users-page [req]
  {:status 200
   :html (layout [:h1 "users"])})


(def routes #{["/" :get
               [html-response front-page] :route-name :front-page]
              ["/users" :get
               [html-response users-page] :route-name :users-page]})

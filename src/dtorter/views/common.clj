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

      [:meta {:name "viewport" :content "width=device-width,initial-scale=1"}]
      [:title (or (str title ", sorter") "sorter")]]
     [:div.topbar
      [:div.topleft
       [:span "sorter"]
       [:a.currentpage {:href (rurl-for ctx :front-page)} "home"]
       (when (= "tommy" (:user-name session))
         (list [:a.currentpage {:href (rurl-for ctx :tdsl-page {:base "tdsl"})} "tdsl"]
               [:a.currentpage {:href (rurl-for ctx :todo-page {:base "todo"})} "todo"]))

       (when false [:a.currentpage {:href 3 #_(url-for :users-page)} "users"])]
      (if-let [username (:user-name session)]
        [:div.topright
         [:a.currentpage {:href (rurl-for ctx :logoff)} "logoff"]
         [:span (:user-name session)]]
        [:div.topright
         [:a.currentpage {:href (rurl-for ctx :login)} "login"]
         [:a.currentpage {:href (rurl-for ctx :register)} "make account"]
         [:span (prn-str session)]])]
     [:div.mainbody
      inner]]))



 (def layout-interceptor
   {:leave (fn [{:keys [response]
                 :as ctx}]
             (if (contains? response :html)
               (assoc-in ctx [:response :html] (layout ctx (:html response)))
               ctx))
    :enter (fn [ctx]
             (assoc-in ctx [:request :href-for] (partial rurl-for ctx)))})



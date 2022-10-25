(ns dtorter.views.common
  (:require [reitit.core :as r]
            [clojure.string :as str]
            [hiccup.page :refer [html5]]
            [babashka.fs :as fs]
            [xtdb.api :as xt]
            [ring.util.response :as ring-resp]))

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
                                   html5
                                   (str "\n"))]
                (assoc ctx :response (-> response
                                         (assoc :body html-body)
                                         (assoc-in [:headers "Content-Type"] "text/html"))))
              ctx))})

(defn find-theme [node userid]
  (ffirst (xt/q (xt/db node) '{:find [(pull theme [*])] :in [userid]
                                      :where [[theme :user userid]
                                              [theme :theme path]]}
                userid)))

(defn change-theme-handler [req]
  (xt/submit-tx (:node req)
                [[::xt/put
                  {:xt/id (or (:xt/id (find-theme (:node req)
                                                  (-> req :session :user-id)))
                              (str (java.util.UUID/randomUUID)))
                   :user (-> req :session :user-id)
                   :theme ((:params req) "theme")}]])
  (ring-resp/redirect (-> req :headers (get "referer"))))

(defn layout [{:keys [request response] :as ctx} inner]
  (let [title (:title response)
        session (-> request :session)
        themes ["/css/dark.css"
                "/css/light.css"
                "/css/bootstrap.css"]
        chosen-theme (or (:theme (find-theme (-> ctx :request :node)
                                             (-> ctx :request :session :user-id)))
                         "/css/dark.css")]

    
    [:html
     [:head
      [:meta {:charset "utf-8"}]
      [:meta {:name "viewport"
              :content "width=device-width, initial-scale=1.0"}]
      [:link {:href chosen-theme
              :rel "stylesheet"
              :type "text/css"}]
      [:script {:src "/js/shared.js"
                :type "text/javascript"}]
      [:script {:type "text/javascript"}
       (str "var userid = '" (-> ctx :request :session :user-id) "';")]

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
      inner]
     [:form {:action "/change_theme" :method "post"}
      [:select {:name "theme"}
       (for [x themes]
         [:option {:value x} x])]
      [:input {:type "submit"}]]]))



 (def layout-interceptor
   {:leave (fn [{:keys [response]
                 :as ctx}]
             (if (contains? response :html)
               (assoc-in ctx [:response :html] (layout ctx (:html response)))
               ctx))
    :enter (fn [ctx]
             (assoc-in ctx [:request :href-for] (partial rurl-for ctx)))})



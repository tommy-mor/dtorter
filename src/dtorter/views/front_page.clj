(ns dtorter.views.front-page
  (:require [io.pedestal.http.route :refer [url-for]]
            [io.pedestal.http.body-params :as body-params]
            [hiccup.core :refer [html]]
            [cryptohash-clj.api :as c]
            [cryptohash-clj.encode :as enc]
            [xtdb.api :as xt]
            [dtorter.hashing :as hashing]
            [ring.util.response :as ring-resp]
            [dtorter.queries :as queries]))


(defn layout [{:keys [session]} inner]
  [:html
   [:head
    [:meta {:charset "utf-8"}]
    [:meta {:name "viewport"
            :content "width=device-width; initial-scale=1.0"}]
    [:link {:href "/css/site.css"
            :rel "stylesheet"
            :type "text/css"}]
    [:script {:src "/js/shared.js"
              :type "text/javascript"}]
    [:title "sorter"]]
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

(def thing (atom nil))
(keys @thing)

(first (queries/all-tags dtorter.http/db))


(defn render-tag [tag]
 [:li.tag.frontpagetag
  [:a {:href (url-for :tag-page :params {:tagid (:xt/id tag)})} (:tag/name tag)]])


(def front-page
  {:name ::front-page
   :enter (fn [{:keys [db request] :as ctx}]
            (let [tags (queries/all-tags db)]
              (assoc ctx
                     :response
                     {:status 200
                      :html (layout request [:div
                                             [:h1 "front page"]
                                             [:ul
                                              (for [tag tags]
                                                (render-tag tag))]])})))})

(defn login-page [req]
  {:status 200
   :html (layout req [:form {:action (url-for :login-submit)
                             :method "POST"}
                      [:input {:required true :type "text" :name "username" :placeholder "user"}]
                      [:input {:required true :type "password" :name "password" :placeholder "pass"}]
                      [:input {:type "submit" :value "login"}]])})

(defn users-page [req]
  {:status 200
   :html (layout req [:h1 "users"])})

(def stest (atom nil))
(:form-params (clojure.pprint/pprint (keys @stest)))

(comment (ffirst (xt/q dtorter.http/db
                '[:find (pull e [*])
                  :in username
                  :where [e :user/name username]]
                "tommy")))

(def login-done
  {:name ::login-done
   :enter (fn [ctx]
            
            (reset! stest ctx)
            (let [{:keys [username password]} (:form-params (:request ctx))
                  user-doc (ffirst (xt/q (:db ctx)
                                         '[:find (pull e [*])
                                           :in username
                                           :where [e :user/name username]]
                                         username))
                  password-hash (:user/password-hash user-doc)]
              (if (and password-hash (hashing/check-pw password password-hash))
                (assoc ctx :response
                       (-> (ring-resp/redirect (url-for :front-page))
                           (assoc :session {:user-id (:xt/id user-doc)
                                            :user-name (:user/name user-doc)})))
                (assoc ctx :response
                       ;; TODO add error msg
                       (login-page (:request ctx))))))})
(def log-off
  {:name ::log-off
   :enter (fn [ctx]
            (-> ctx
                (assoc :response (-> (ring-resp/redirect (url-for :front-page))
                                     (assoc :session nil)))))})

(:path-params (:request @stest))

(def tag-page
  {:name ::tag-page
   :enter (fn [{:keys [db request] :as ctx}]
            (let [tid (-> request :path-params :tagid)
                  tag (queries/tag-by-id db tid)]
              (reset! stest ctx)
              (assoc ctx :response {:status 200 :html
                                    (layout request [:div
                                                     [:h1 "tag page"]
                                                     [:span (prn-str tag)]])})))})



(defn routes [common-interceptors]
  #{["/" :get
     (into common-interceptors [front-page]) :route-name :front-page]
    ["/users" :get
     (into common-interceptors [users-page]) :route-name :users-page]
    ["/login" :get
     (into common-interceptors [login-page]) :route-name :login-page]
    ["/login" :post
     (into common-interceptors [(body-params/body-params) login-done]) :route-name :login-submit]
    ["/logoff" :get
     (into common-interceptors [log-off]) :route-name :log-off]

    ["/t/:tagid" :get
     (into common-interceptors [tag-page]) :route-name :tag-page]})

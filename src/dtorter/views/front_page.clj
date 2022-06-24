(ns dtorter.views.front-page
  (:require [io.pedestal.http.route :refer [url-for]]
            [io.pedestal.http.body-params :as body-params]
            [hiccup.core :refer [html]]
            [cryptohash-clj.api :as c]
            [cryptohash-clj.encode :as enc]
            [xtdb.api :as xt]
            [dtorter.hashing :as hashing]
            [ring.util.response :as ring-resp]
            [dtorter.queries :as queries]
            [cheshire.core :as json]
            [dtorter.views.tag :as tag]
            [dtorter.views.common :refer [layout]]

            [tdsl.show]))


(defn render-tag [tag]
  [:li.tag.frontpagetag
   [:a {:href 3 #_(url-for :tag-page :params {:tagid (:xt/id tag)})} (:tag/name tag)]])


(defn page [request]
  (def request request)
  (layout request [:div
                   [:h1 "front page"]
                   [:ul
                    (for [tag [3 4 5]]
                      (render-tag tag))]]
          {:title "sorter frontpage"}))

(defn login-page [req]
  {:status 200
   :html (layout req [:form {:action 3 #_(url-for :login-submit)
                             :method "POST"}
                      [:input {:required true :type "text" :name "username" :placeholder "user"}]
                      [:input {:required true :type "password" :name "password" :placeholder "pass"}]
                      [:input {:type "submit" :value "login"}]]
                 {:title "sorter login"})})

(defn users-page [req]
  {:status 200
   :html (layout req [:h1 "users"])})

(def login-done
  {:name ::login-done
   :enter (fn [ctx]
            
            (let [{:keys [username password]} (:form-params (:request ctx))
                  user-doc (ffirst (xt/q (xt/db (:node ctx))
                                         '[:find (pull e [*])
                                           :in username
                                           :where [e :user/name username]]
                                         username))
                  password-hash (:user/password-hash user-doc)]
              (if (and password-hash (hashing/check-pw password password-hash))
                (assoc ctx :response
                       (-> (ring-resp/redirect 3 #_(url-for :front-page))
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

;; TODO put these into arguments to init/bang, not here
(defn routes [common-interceptors]
  (into #{["/" :get
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
           (into common-interceptors [tag/tag-page]) :route-name :tag-page]
          ["/t/:tagid/:itemid" :get
           (into common-interceptors [tag/item-page]) :route-name :item-page]}
        (tdsl.show/routes common-interceptors)))



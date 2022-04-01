(ns dtorter.views.front-page
  (:require [io.pedestal.http.route :refer [url-for]]
            [io.pedestal.http.body-params :as body-params]
            [hiccup.core :refer [html]]
            [cryptohash-clj.api :as c]
            [cryptohash-clj.encode :as enc]
            [xtdb.api :as xt]
            [dtorter.hashing :as hashing]))

;; for now, just replace every password with default password, can be updated later.

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
     [:a.currentpage {:href (url-for :login-page)} "login"]
     [:a.currentpage "make account"]]
    [:div.mainbody
     inner]]])

(defn front-page [req]
  {:status 200
   :html (layout [:h1 "inssneruu"])})

(defn login-page [req]
  {:status 200
   :html (layout [:form {:action (url-for :login-submit)
                         :method "POST"}
                  [:input {:required true :type "text" :name "username" :placeholder "user"}]
                  [:input {:required true :type "password" :name "password" :placeholder "pass"}]
                  [:input {:type "submit" :value "login"}]])})

(defn users-page [req]
  {:status 200
   :html (layout [:h1 "users"])})

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
              (if (hashing/check-pw password password-hash)
                (assoc ctx :response
                       {:status 200
                        :html (layout [:h1 "epic"])})
                (assoc ctx :response
                       {:status 300
                        :html (layout [:h1 "cringe"])}))))})

(defn routes [load-db]
  #{["/" :get
     [html-response front-page] :route-name :front-page]
    ["/users" :get
     [html-response users-page] :route-name :users-page]
    ["/login" :get
     [html-response login-page] :route-name :login-page]
    ["/login" :post
     [html-response (body-params/body-params) load-db login-done] :route-name :login-submit]})

(ns dtorter.views.login
  (:require
   [dtorter.views.common :as c]
   [xtdb.api :as xt]
   [dtorter.hashing :as hashing]
   [ring.util.response :as ring-resp]
   [tick.core :as t]))

(def login-page
  {:leave (fn [ctx]
            (assoc-in
             ctx
             [:response :html]
             [:form {:action (c/rurl-for ctx :login)
                     :method "POST"}
              [:input {:required true :type "text" :name "username" :placeholder "user"}]
              [:input {:required true :type "password" :name "password" :placeholder "pass"}]
              [:input {:type "submit" :value "login"}]]))})

(def register-page
  {:leave (fn [ctx]
            (assoc-in
             ctx
             [:response :html]
             [:form {:action (c/rurl-for ctx :register)
                     :method "POST"}
              [:input {:required true :type "text" :name "username" :placeholder "user"}]
              [:input {:required true :type "password" :name "password" :placeholder "pass"}]
              [:input {:required true :type "password" :name "password-again" :placeholder "pass"}]
              [:input {:type "submit" :value "register"}]]))})

(def login-done
  {:name ::login-done
   :leave (fn [ctx]
            (def ctx ctx)
            (-> ctx :request
                :form-params)
            
            
            (-> (let [form (:form-params (:request ctx))
                      username (form "username")
                      password (form "password")
                      user-doc (ffirst (xt/q (xt/db (-> ctx :request :node))
                                             '[:find (pull e [*])
                                               :in username
                                               :where [e :user/name username]]
                                             username))
                      password-hash (:user/password-hash user-doc)
                      cookie-expires (t/format "EEE, dd MMM yyyy HH:mm:ss z"
                                               (-> t/date-time
                                                   t/zoned-date-time
                                                   (t/>>
                                                    (t/new-period 1 :months))
                                                   (t/in "GMT")))]
                  (if (and password-hash (hashing/check-pw password password-hash))
                    (assoc ctx :response
                           (-> (ring-resp/redirect (c/rurl-for ctx :front-page))
                               (assoc :session {:user-id (:xt/id user-doc)
                                                :user-name (:user/name user-doc)}
                                      :session-cookie-attrs {:expires cookie-expires})))
                    ((:leave login-page) ctx)))))})
(def register-done
  {:name ::register-done
   :leave (fn [ctx]
            (def ctx ctx)

            (def form
              (-> ctx
                  :request
                  :form-params))
            (def existing-user (ffirst (xt/q (xt/db (-> ctx :request :node))
                                             '[:find (pull e [*])
                                               :in username
                                               :where [e :user/name username]]
                                             (form "username"))))
            (if existing-user
              (assoc ctx :response))
            
            
            (-> (let [form (:form-params (:request ctx))
                      username (form "username")
                      password (form "password")
                      user-doc (ffirst (xt/q (xt/db (-> ctx :request :node))
                                             '[:find (pull e [*])
                                               :in username
                                               :where [e :user/name username]]
                                             username))
                      password-hash (:user/password-hash user-doc)
                      
                      
                      ]
                  (if (and password-hash (hashing/check-pw password password-hash))
                    (assoc ctx :response
                           (-> (ring-resp/redirect (c/rurl-for ctx :front-page))
                               (assoc :session {:user-id (:xt/id user-doc)
                                                :user-name (:user/name user-doc)})))
                    ((:leave login-page) ctx)))))})

(defn users-page [req]
  {:status 200
   :html [:h1 "users"]})

(def log-off
  {:name :logoff
   :enter (fn [ctx]
            (-> ctx
                (assoc :response (-> (ring-resp/redirect (c/rurl-for ctx :front-page))
                                     (assoc :session nil)))))})

(defn login-routes []
  [["/login" {:name :login
              :get {:interceptors [login-page]
                    :handler (constantly {:status 200})}
              :post {:interceptors [login-done]
                     :handler (constantly {:status 200})}}]
   ["/register" {:name :register
                 :get {:interceptors [register-page]
                       :handler (constantly {:status 200})}
                 :post {:interceptors [register-done]
                        :handler (constantly {:status 200})}}]
   ["/logoff"
    {:name :logoff
     :get {:interceptors [log-off]
           :handler (constantly {:status 200}) }}]])

(ns dtorter.views.login
  (:require
   [dtorter.views.common :as c]
   [xtdb.api :as xt]
   [dtorter.hashing :as hashing]
   [ring.util.response :as ring-resp]))

(def login-page
  {:leave (fn [ctx]
            (println "uhh")
            (assoc-in
             ctx
             [:response :html]
             [:form {:action (c/rurl-for ctx :login)
                     :method "POST"}
              [:input {:required true :type "text" :name "username" :placeholder "user"}]
              [:input {:required true :type "password" :name "password" :placeholder "pass"}]
              [:input {:type "submit" :value "login"}]]))})

(def login-done
  {:name ::login-done
   :leave (fn [ctx]
            (println "uhh")
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
                      password-hash (:user/password-hash user-doc)]
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
                    :handler (fn [req] {:status 200})}
              :post {:interceptors [login-done]
                     :handler (fn [req] {:status 200})}}]
   ["/logoff"
    {:name :logoff
     :get {:interceptors [log-off]
           :handler (constantly {:status 200}) }}]])

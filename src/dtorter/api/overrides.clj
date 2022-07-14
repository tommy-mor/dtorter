(ns dtorter.api.overrides
  (:require
   [xtdb.api :as xt]
   [dtorter.api.common :refer [document-interceptor]]
   [shared.specs :as sp]
   [dtorter.queries :as queries]
   [clojure.spec.alpha :as s]
   [dtorter.hashing :as hashing]))

(def no-changes {:all identity :individual identity :extra-routes []})

(def user
  {:individual
   #(assoc %
           :get
           {:handler (fn [{:keys [resource]}]
                       {:status 200 :body
                        (dissoc
                         resource
                         :user/password-hash)})
            :summary "get a user"
            :operationId :user/get}) 
   :all (constantly
         {:get {:operationId :user/list-all
                :summary "list all usernames"
                :handler
                (fn [req]
                  (def req req)
                  req
                  {:status 200 :body (map first (xt/q (xt/db (:node req))
                                                      '[:find (pull e [:user/name :xt/id])
                                                        :where [e :type :user]]))})}
          :post {:operationId :user/new
                 :parameters {:body (s/keys :req [:user/name :user/password])}
                 :summary "register a new user"
                 :handler
                 (fn [req]
                   (def req req)
                   (def username (-> req :body-params :user/name))

                   (def uuid (str (java.util.UUID/randomUUID)))
                   
                   (if-not (xt/pull (xt/db (:node req)) [:user] username)
                     (do
                       (xt/await-tx (:node req)
                                    (xt/submit-tx (:node req)
                                                  [[::xt/put
                                                    {:xt/id username :user uuid}]
                                                   
                                                   [::xt/put
                                                    {:type :user
                                                     :user/name username
                                                     
                                                     :user/password-hash
                                                     (hashing/hash-pw (-> req :body-params :user/password))
                                                     
                                                     :xt/id uuid}]]))
                       {:status 201 :body {:xt/id uuid}})
                     {:status 400 :body "username taken"}))}})})

(def vote
  {:all #(dissoc % :get)})

(def item
  {:all #(dissoc % :get)})

;; todo add response spec checking in reitit...
(def tag
  {:extra-routes
   [["/:id/items"
     {:get {:operationId :tag/items
            :summary "get all the items added to this tag"
            :parameters {:path {:id string?}}
            :interceptors [(document-interceptor ::sp/tag)]
            :handler (fn [req]
                       (def req req)
                       (let [{:keys [node path-params]} req
                             tid (:id path-params)]
                         {:status 200
                          :body (map first
                                     (xt/q
                                      (xt/db node) '[:find
                                                     (pull iid [*])
                                                     :in tid
                                                     :where
                                                     [iid :item/tags tid]] tid))}))}}]
    ["/:id/sorted"
     {:get {:operationId :tag/sorted
            :summary "get all the calculated information about a tag"
            :parameters {:path {:id string?}
                         :query ::sp/tag-query}
            :handler
            (fn [req]
              {:status 200 :body (queries/tag-info req)})}}]]})



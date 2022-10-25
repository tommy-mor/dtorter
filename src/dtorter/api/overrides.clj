(ns dtorter.api.overrides
  (:require
   [xtdb.api :as xt]
   [dtorter.api.common :refer [document-interceptor]]
   [shared.specs :as sp]
   [dtorter.queries :as queries]
   [clojure.spec.alpha :as s]
   [dtorter.hashing :as hashing]
   [clojure.string :as str]))

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
                  {:status 200 :body (map first (xt/q (xt/db (:node req))
                                                      '[:find (pull e [:user/name :xt/id])
                                                        :where [e :type :user]]))})}
          :post {:operationId :user/new
                 :parameters {:body (s/keys :req [:user/name :user/password])}
                 :summary "register a new user"
                 :handler
                 (fn [req]
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

(def vote-upsert
  {:enter
   (fn vote-upsert [ctx]
     
     (def ctx ctx)
     (def req (ctx :request))
     (let [uuid (or (ffirst (xt/q (xt/db (:node req)) '{:find [uuid]
                                                        :in [tagid attribute owner id id2]
                                                        :where
                                                        [[e :vote/tag tagid]
                                                         [e :vote/attribute attribute]
                                                         [e :owner owner]
                                                         (or (and [e :vote/left-item id]
                                                                  [e :vote/right-item id2])
                                                             (and [e :vote/left-item id2]
                                                                  [e :vote/right-item id]))
                                                         [e :xt/id uuid]]}
                                  (-> req :body-params :vote/tag)
                                  (-> req :body-params :vote/attribute)
                                  (-> req :body-params :owner)
                                  (-> req :body-params :vote/left-item)
                                  (-> req :body-params :vote/right-item)))
                    (str (java.util.UUID/randomUUID)))]
       (assoc-in ctx [:request :body-params :xt/id] uuid)))})

(def vote
  {:all #(-> %
             (dissoc :get)
             (assoc-in [:post :interceptors] [vote-upsert]))})

(def add-item
  {:operationId :item/new
   :summary "create an item (on a tag)"
   :parameters {:body ::sp/item}
   :handler
   (fn [req]
     (let [{:keys [node body-params]} req]
       (def node node)
       (def req req)
       (def body-params body-params)
       (def uuid (if (and (:item/url body-params) (str/starts-with? (:item/url body-params) "https://youtube.com"))
                   (str "yt_" (str/replace (:item/url body-params) "https://youtube.com/watch?v=" ""))
                   (str (java.util.UUID/randomUUID))))

       (let [doc (xt/entity (xt/db node) uuid)
             membership
             (vec (for [tag (:item/tags body-params)]
                    [::xt/put {:xt/id (str (java.util.UUID/randomUUID))
                               :type :membership
                               :tag tag
                               :item uuid
                               :owner (:owner body-params)}]))]
         (if doc
           (xt/submit-tx node membership)
           (xt/submit-tx node
                         (into membership
                               [
                                [::xt/put
                                 (-> body-params
                                     (assoc :xt/id uuid :type :item)
                                     (dissoc :item/tags))]]))))
       {:status 201 :body {:xt/id uuid}}))})

(def item
  {:all #(-> %
             (dissoc :get)
             (assoc :post add-item))
   :extra-routes
   [["/:id/memberships"
     {:get {:operationId :item/memberships
            :summary "get all the tags that this item is in"
            :parameters {:path {:id string?}}
            :interceptors [(document-interceptor ::sp/item)]
            :handler (fn [req]
                       (def req req)
                       {:status 200
                        :body (map second (xt/q (xt/db (:node req))
                                                '{:find [(pull owner [:user/name :xt/id])
                                                         (pull tag [*])]
                                                  :in [item]
                                                  :where [[memb :type :membership]
                                                          [memb :item item]
                                                          [memb :tag tag]
                                                          [memb :owner owner]]}
                                                (-> req :path-params :id)))})}
      :post {:operationId :item/join-tag
             :summary "add an item to a new tag"
             :parameters {:path {:id string?}}
             :handler (fn [req]
                        (def req req)
                        {:status 200
                         :body {:done "yep"}})} }]]})

;; todo add response spec checking in reitit...
(def tag
  {:extra-routes
   [["/:id/items"
     {:get {:operationId :tag/items
            :summary "get all the items added to this tag"
            :parameters {:path {:id string?}}
            :interceptors [(document-interceptor ::sp/tag)]
            :handler (fn [req]
                       (let [{:keys [node path-params]} req
                             tid (:id path-params)]

                         (def req req)
                         (def tid (-> req :path-params :id))
                         {:status 200
                          :body (map first
                                     (xt/q
                                      (xt/db node) '[:find
                                                     (pull iid [*])
                                                     :in tid
                                                     :where
                                                     [memb :type :membership]
                                                     [memb :tag tid]
                                                     [memb :item iid]] tid))}))}}]
    ["/:id/sorted"
     {:get {:operationId :tag/sorted
            :summary "get all the calculated information about a tag"
            :parameters {:path {:id string?}
                         :query ::sp/tag-query}
            :handler
            (fn [req]
              {:status 200 :body (queries/tag-info req)})}}]]})



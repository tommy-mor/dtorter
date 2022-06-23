(ns dtorter.api.overrides
  (:require
   [xtdb.api :as xt]
   [dtorter.api.common :refer [document-interceptor]]
   [shared.specs :as sp]))

(def no-changes {:all identity :individual identity :extra-routes []})

(def vote
  {:individual #(merge %
                       {:get
                        {:handler (fn [{:keys [resource]}] {:status 200 :body resource})
                         :summary (str "get a vote epicly")
                         :operationId (keyword "vote" "get")}})
   :all #(dissoc % :get)
   :extra-routes []})

(def item
  {:individual
   #(assoc
     %
     :post
     ;; do custom logic to check for tag existence,
     ;; tag permissions, 
     {})
   :all #(dissoc % :get)
   :extra-routes []})

;; todo add response spec checking in reitit...
(def tag
  {:individual identity
   :all identity
   :extra-routes
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
            :sumamry "get all the calculated information about a tag"
            :handler
            (fn [req]
              (def req req)
              (let [{:keys [node path-params]} req
                    tid 3 ]
                (comment "TODO PUT get-info HERE")))}}]]})
(def routes-todo
  {"/api/tag/{id}/items" "get only, item things all stay in item resource.."})

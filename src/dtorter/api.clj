(ns dtorter.api
  (:require [clojure.java.io :as io]
            [clojure.edn  :as edn]
            [clojure.walk :refer [postwalk]]

            [dtorter.queries :as queries]
            [dtorter.mutations :as mutations]
            [dtorter.math :as math]
            [dtorter.util :refer [strip]]
            [xtdb.api :as xt]

            [shared.specs :as sp]
            [clojure.spec.alpha :as s]))


(def api-interceptors
  [])

(defn document-interceptor [spec]
  {:enter (fn [ctx]
            (def ctx ctx)
            (let [req (ctx :request)
                  method (req :request-method)
                  id (-> req :path-params :id)
                  userid (-> ctx :TODO)
                  node (req :node)
                  
                  doc (xt/pull (xt/db node) '[*] id)
                  allowed-methods #{:get :post :delete :put}]

              (def id id)
              (def spec spec)
              (def doc doc)
              
              (if-not doc
                (throw (ex-info (format "document does not exist") req)))

              ;; TODO make this spec check...
              (if-not (s/valid? spec doc)
                (throw (ex-info (format "document id is not type %s" spec) req)))
              
              ;; TODO make 503
              (if-not (allowed-methods method)
                (throw (ex-info "method not allowed" req)))
              
              (-> ctx
                  (assoc-in [:request :resource] doc))))})

(defn crud-methods [swagger-tag spec queries]
  [(str "/" swagger-tag)
   {:swagger {:tags [swagger-tag]}}
   [["/"
     {:post {:operationId (keyword swagger-tag "new")
             :summary (str "create a " swagger-tag)
             :parameters {:body spec}
             :handler (fn [req]
                        (let [{:keys [node body-params]} req
                              uuid (str (java.util.UUID/randomUUID))]
                          (xt/submit-tx node [[::xt/put (assoc body-params :xt/id uuid)]])
                          {:status 201 :body {:xt/id uuid}}))}
      
      :get {:operationId (keyword swagger-tag "list-all")
            :summary (str "list all " swagger-tag "s")
            :handler (fn [req]
                       (def identifying-attribute identifying-attribute)
                       (def req req)
                       (let [{:keys [node]} req]
                         {:status 200
                          :body
                          (into [] ((:get-all queries) (:node req)))}))}}]
    ["/:id"
     {:parameters {:path {:id string?}}
      :interceptors [(document-interceptor spec)]
      :get {:handler (fn [{:keys [resource]}] {:status 200 :body resource})
            :summary (str "get a " swagger-tag)
            :operationId (keyword swagger-tag "get")}
      :put {:parameters {:body spec}
            :handler
            (fn [{:keys [resource node body-params] :as ctx}]
              (xt/submit-tx node [[::xt/put (assoc body-params :xt/id (-> ctx :path-params :id))]])
              {:status 204 :body "received"})
            :summary (str "update/replace a " swagger-tag)
            :operationId (keyword swagger-tag "put")}
      :delete {:handler (fn [{:keys [resource node]}]
                          (xt/submit-tx node [[::xt/delete (:xt/id resource)]])
                          {:status 204})
               :summary (str "delete a " swagger-tag)
               :operationId (keyword swagger-tag "delete")}
      
      :strst
      {:handler (fn [{:keys [node] :as req}]
                  ;; try to put as much logic as possible in xtdb..
                  ;; try to put as much into tag-interceptor...
                  ;; maybe move :tag/owner into just :owner , so permission logic can be general
                  ;; have query option to include 
                  ;; every high level interceptor does a "protected pull" of the document.
                  ;; which calculates which http verbs you can perform on it.
                  ;;   it checks the verb with :request :request-verb and fails/passes.
                  ;; if you have the document in any lower handler you can perform operation..
                  ;; query param on get, if it includes filter, then include expanded data
                  ;; queries can include special parameter which is like a piggyback request,
                  ;; for single round trip state updates for frontend
                  {:status 200 :body (xt/pull (xt/db node) '[*] (-> req :path-params :id))})}}]]])
(def api-routes
  [
   (crud-methods "tag" ::sp/tag queries/tag-queries)
   (crud-methods "item" ::sp/item queries/item-queries)
   (crud-methods "vote" ::sp/vote queries/vote-queries)])

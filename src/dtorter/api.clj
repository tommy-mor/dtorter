(ns dtorter.api
  (:require [dtorter.queries :as queries]
            [dtorter.math :as math]
            
            [xtdb.api :as xt]

            [shared.specs :as sp]
            [clojure.spec.alpha :as s]
            [dtorter.api.overrides :as overrides]
            [dtorter.api.common :refer [document-interceptor]]))


(def swag-interceptor
  {:enter #(assoc % :swag 3)
   :leave (fn [ctx]
            (def ctx ctx)
            (-> ctx :response)
            (assoc-in ctx [:response :body] {:response (-> ctx :response :body)
                                             :taginfo 3 })
            ctx)})

(def api-interceptors [swag-interceptor])


;; has to mess with arguments, cause vote api endpoint has more parameters..
;; only at route creation time for now..
(defn crud-methods [swagger-tag spec queries overrides]
  [(str "/" swagger-tag)
   {:swagger {:tags [swagger-tag]}}
   (into [["/"
           ((:all overrides)
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
                              (def req req)
                              (let [{:keys [node]} req]
                                {:status 200
                                 :body
                                 (into [] ((:get-all queries) (:node req)))}))}})]
          ["/:id"
           ((:individual overrides)
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
                      :operationId (keyword swagger-tag "delete")}})]]
         (:extra-routes overrides))])

(def api-routes
  [
   (crud-methods "tag" ::sp/tag queries/tag-queries overrides/tag)
   (crud-methods "item" ::sp/item queries/item-queries overrides/item) 
   (crud-methods "vote" ::sp/vote queries/vote-queries dtorter.api.overrides/vote)])

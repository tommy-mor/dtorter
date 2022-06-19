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
            (let [req (ctx :request)
                  method (req :request-method)
                  id (-> req :path-params :id)
                  userid (-> ctx :TODO)
                  node (req :node)
                  
                  doc (xt/pull (xt/db node) '[*] id)
                  allowed-methods #{:get :post :delete :put}]

              (def id id)
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

#_(s/explain ::sp/tag {:tag/name "strts" :xt/id "rtsrs"} )
(defn crud-methods [swagger-tag spec]
  {:parameters {:path {:id string?}}
   :interceptors [(document-interceptor spec)]
   :swagger {:tags [swagger-tag]}

   :get {:handler (fn [{:keys [resource]}] {:status 200 :body resource})}
   :put {:parameters {:body spec}
         :handler
         (fn [{:keys [resource node body-params] :as ctx}]
           (xt/submit-tx node [[::xt/put body-params]])
           {:status 204 :body "received"})}
   
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
               {:status 200 :body (xt/pull (xt/db node) '[*] (-> req :path-params :id))})}}
  )
(def api-routes
  [
   ["/tag/:id"
    (crud-methods "tag" ::sp/tag)]
   
   ["/item/:id"
    (crud-methods "item" ::sp/item)]])

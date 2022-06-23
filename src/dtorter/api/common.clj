(ns dtorter.api.common
  (:require
   [xtdb.api :as xt]
   [clojure.spec.alpha :as s]
   [shared.specs :as sp]))

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

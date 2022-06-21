(ns dtorter.swagger
  (:require [reitit.core :as r]
            [reitit.trie :as trie]
            [clojure.set :as set]
            [meta-merge.core :refer [meta-merge]]
            [reitit.coercion :as coercion]
            [clojure.string :as str]))


;; custom version of swagger handler that includes operationId.
;; until https://github.com/metosin/reitit/pull/452 gets merged


(defn- swagger-path [path opts]
  (-> path (trie/normalize opts) (str/replace #"\{\*" "{")))

(defn create-swagger-handler
  "Create a ring handler to emit swagger spec. Collects all routes from router which have
  an intersecting `[:swagger :id]` and which are not marked with `:no-doc` route data."
  []
  (fn create-swagger
    ([{::r/keys [router match] :keys [request-method]}]
     (let [{:keys [id] :or {id ::default} :as swagger} (-> match :result request-method :data :swagger)
           ids (trie/into-set id)
           strip-top-level-keys #(dissoc % :id :info :host :basePath :definitions :securityDefinitions)
           strip-endpoint-keys #(dissoc % :id :parameters :responses :summary :description :operationId)
           swagger (->> (strip-endpoint-keys swagger)
                        (merge {:swagger "2.0"
                                :x-id ids}))
           accept-route (fn [route]
                          (-> route second :swagger :id (or ::default) (trie/into-set) (set/intersection ids) seq))
           base-swagger-spec {:responses ^:displace {:default {:description ""}}}
           transform-endpoint (fn [[method {{:keys [coercion no-doc swagger] :as data} :data
                                            middleware :middleware
                                            interceptors :interceptors}]]
                                (if (and data (not no-doc))
                                  [method
                                   (meta-merge
                                    base-swagger-spec
                                    (apply meta-merge (keep (comp :swagger :data) middleware))
                                    (apply meta-merge (keep (comp :swagger :data) interceptors))
                                    (if coercion
                                      (coercion/get-apidocs coercion :swagger data))
                                    (select-keys data [:tags :summary :description :operationId])
                                    (strip-top-level-keys swagger))]))
           transform-path (fn [[p _ c]]
                            (if-let [endpoint (some->> c (keep transform-endpoint) (seq) (into {}))]
                              [(swagger-path p (r/options router)) endpoint]))
           map-in-order #(->> % (apply concat) (apply array-map))
           paths (->> router (r/compiled-routes) (filter accept-route) (map transform-path) map-in-order)]
       {:status 200
        :body (meta-merge swagger {:paths paths})}))
    ([req res raise]
     (try
       (res (create-swagger req))
       (catch #?(:clj Exception :cljs :default) e
         (raise e))))))

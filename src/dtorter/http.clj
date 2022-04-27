(ns dtorter.http
  (:require [io.pedestal.http :as server]
            [io.pedestal.http.route :as route]
            [io.pedestal.test :as test]
            [com.walmartlabs.lacinia.pedestal2 :as lp]
            
            [dtorter.api :as api]
            [dtorter.queries :as queries]
            [dtorter.data :as data]
            [xtdb.api :as xt]

            [dtorter.views.front-page :as views]
            [hiccup.core :refer [html]]
            
            [ring.middleware.session.cookie :as cookie]
            [io.pedestal.http.ring-middlewares :as middlewares]
            [clojure.java.io :as io]))
;; TODO clean up this file by using
;; https://lacinia.readthedocs.io/en/latest/tutorial/component.html
;; this library/tutorial

(def schema (api/load-schema))

;; database stuff


(defn lmdb-at [f] {:kv-store {:xtdb/module 'xtdb.lmdb/->kv-store
                              :db-dir (io/file f)}})
(comment (def node (xt/start-node
            {:xtdb/index-store (lmdb-at "/tmp/idx")
             :xtdb/document-store (lmdb-at "/tmp/ds")
             :xtdb/tx-log (lmdb-at "/tmp/log")})))
(def node (xt/start-node {}))
(def db (do
          (xt/submit-tx node (for [tx (data/get-transactions)]
                               [::xt/put tx]))
          (xt/sync node)
          (xt/db node)))
(def cookies (middlewares/session {:store (cookie/cookie-store)}))

(defn enable-graphql [service-map schema]
  (let [interceptors (into [cookies] (lp/default-interceptors schema {:db db :node node}))]
    (-> service-map
        (update ::server/routes conj
                ["/api" :post interceptors :route-name ::graphql-api]))))

(defn enable-ide [service-map schema]
  (-> service-map
      (update ::server/routes conj
              ["/ide" :get (lp/graphiql-ide-handler {}) :route-name ::graphql-ide])
      (update ::server/routes into
              (lp/graphiql-asset-routes "/assets/graphiql"))
      (assoc ::server/secure-headers nil)))

(def service
  (-> {:env :prod
       ::server/type :jetty
       ::server/port 8080
       ::server/resource-path "/public"}
      (enable-graphql schema)))

(def common-interceptors
  [cookies
   {:name ::load-db
    :enter (fn [ctx]
             (assoc ctx
                    :db (xt/db node)
                    :node node))}
   {:name ::load-gql-schema
    :enter (fn [ctx]
             (assoc ctx :gql-schema schema))}



   ;; https://souenzzo.com.br/clojure-ssr-app-with-pedestal-and-hiccup.html
   {:name  ::html-response
    :leave (fn [{:keys [response]
                 :as   ctx}]
             (if (contains? response :html)
               (let [html-body (->> response
                                    :html
                                    html
                                    (str "\n"))]
                 (assoc ctx :response (-> response
                                          (assoc :body html-body)
                                          (assoc-in [:headers "Content-Type"] "text/html"))))
               ctx))}])

(defn refresh-routes []
  "regather routes for dev thing"
  (let [schema (api/load-schema)]
    
    (route/expand-routes
     (::server/routes (-> {::server/routes (views/routes common-interceptors)}
                          (enable-graphql schema)
                          (enable-ide schema))))))

    


;; taken from https://github.com/pedestal/pedestal/blob/50fe5ea89108998ac5a79a02a44432fd111ea6f8/samples/json-api/src/json_api/server.clj#L11
(defn run-dev
  "runs a service for the terminal"
  []
  (-> service
      (enable-ide schema)
      (merge {:env :dev
              ::server/join? false
              ::server/routes refresh-routes
              ::server/allowed-origin {:creds true :allowed-origins (constantly true)}})
      server/default-interceptors
      server/dev-interceptors
      server/create-server
      server/start))

(defonce server (atom nil))

(defn stop-server [server] (server/stop server))

(defn start-server [old]
  (when old (stop-server old))
  (run-dev))

(defn start [] (swap! server start-server))
(defn stop [] (swap! server stop-server))
(comment
  (start)

  (:io.pedestal.http/interceptors @server)

  (stop)
  (start))

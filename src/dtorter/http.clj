(ns dtorter.http
  (:require [io.pedestal.http :as server]
            [io.pedestal.http.route :as route]
            [com.walmartlabs.lacinia.pedestal2 :as lp]
            
            [dtorter.views.front-page :as views]
            [hiccup.core :refer [html]]
            
            [ring.middleware.session.cookie :as cookie]
            [io.pedestal.http.ring-middlewares :as middlewares]
            
            [com.stuartsierra.component :as component]))


(def cookies (middlewares/session {:store (cookie/cookie-store)}))

(defn enable-graphql [service-map schema]
  (let [interceptors (into [cookies] (lp/default-interceptors schema {}))]
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


(defn common-interceptors [schema node]
  [cookies
   {:name ::load-gql-schema
    :enter (fn [ctx]
             (assoc ctx
                    :gql-schema schema
                    :node node))}

   
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

(defrecord Server [server schema-provider db]
  component/Lifecycle
  (start [this]
    (println "starting server")
    (assoc this :server
           (-> {:env :dev
                ::server/type :jetty
                ::server/port 8080
                ::server/resource-path "/public"
                ::server/routes (views/routes (common-interceptors (:schema schema-provider)
                                                                   (:node db)))}
               
               (enable-graphql (:schema schema-provider))
               (enable-ide (:schema schema-provider))

               (update ::server/routes route/expand-routes)
               
               (merge {::server/join? false
                       ::server/allowed-origin {:creds true :allowed-origins (constantly true)}})

               

               server/default-interceptors
               server/dev-interceptors
               server/create-server
               server/start)))
  
  (stop [this]
    (server/stop (:server this))
    (assoc this :server nil)))

(defn new-server []
  (map->Server {}))

(ns dtorter.http
  (:require [io.pedestal.http :as server]
            [io.pedestal.http.route :as route]
            [com.walmartlabs.lacinia.pedestal2 :as lp]
            
            [dtorter.views.front-page :as views]
            [hiccup.core :refer [html]]
            
            [ring.middleware.session.cookie :as cookie]
            [io.pedestal.http.ring-middlewares :as middlewares]
            [dtorter.db :as db]
            [dtorter.api]))


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

(defonce server (atom nil))

(def node (dtorter.db/start))
(def resolver (dtorter.api/load-schema node))



(defn start [node api]
  (-> {:env :dev
       ::server/type :jetty
       ::server/port 8080
       ::server/resource-path "/public"
       ::server/routes (views/routes (common-interceptors resolver
                                                          node))}
      
      (enable-graphql resolver)
      (enable-ide resolver)

      #_(update ::server/routes route/expand-routes)
      (assoc ::server/routes #(let [resolver (dtorter.api/load-schema node)]
                                (-> {::server/routes
                                     (views/routes (common-interceptors
                                                    resolver node))}
                                    (enable-graphql resolver)
                                    (enable-ide resolver)
                                    ::server/routes
                                    route/expand-routes)))
      
      (merge {::server/join? false
              ::server/allowed-origin {:creds true :allowed-origins (constantly true)}})

      

      server/default-interceptors
      server/dev-interceptors
      server/create-server
      server/start
      (as-> $ (reset! server $))))

(defn stop []
  (server/stop @server))

(defn reset []
  (when @server
    (stop))
  (start node resolver))
@server

(comment (reset))





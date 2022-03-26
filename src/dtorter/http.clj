(ns dtorter.http
  (:require [io.pedestal.http :as server]
            [io.pedestal.http.route :as route]
            [io.pedestal.test :as test]
            [com.walmartlabs.lacinia.pedestal2 :as lp]
            [dtorter.api :as api]
            [dtorter.queries :as queries]))

(def schema (api/load-schema))

;; database stuff

; (def conn (dtorter.queries/get-conn))

(defn enable-graphql [service-map schema]
  (let [interceptors (lp/default-interceptors schema {:conn true})]
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

(def routes #{["/test" :get
               (fn [r] {:status 200 :body (str "hssellos world")})
               :route-name :bingus]})
(def service
  (-> {:env :prod
       ::server/type :jetty
       ::server/port 8080}
      (enable-graphql schema)))

(defn refresh-routes []
  "regather routes for dev thing"
  (let [schema (api/load-schema)]
    
    (route/expand-routes
     (::server/routes (-> {::server/routes routes}
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

  @server

  (stop)
  (start))


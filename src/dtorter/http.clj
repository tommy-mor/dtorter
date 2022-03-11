(ns dtorter.http
  (:require [io.pedestal.http :as server]
            [io.pedestal.http.route :as route]
            [io.pedestal.test :as test]
            [com.walmartlabs.lacinia.pedestal2 :as lp]
            [dtorter.api :as api]))

(def schema (api/load-schema))

(defn enable-graphql [service-map]
  (let [interceptors (lp/default-interceptors schema {:this-is-the-app-context true})]
    (-> service-map
        (update ::server/routes conj
                ["/api" :post interceptors :route-name ::graphql-api]))))

(defn enable-ide [service-map]
  (-> service-map
      (update ::server/routes conj
              ["/ide" :get (lp/graphiql-ide-handler {}) :route-name ::graphql-ide])
      (update ::server/routes into
              (lp/graphiql-asset-routes "/assets/graphiql"))
      (assoc ::server/secure-headers nil)))

(def routes #{["/test" :get (fn [r] {:status 200 :body (str "hssellos world")}) :route-name :bingus]})
(def service
  (-> {:env :prod
       ::server/type :jetty
       ::server/port 8080
       
       }
      enable-graphql))

(defn refresh-routes []
  "regather routes for dev thing"
  (route/expand-routes
   (::server/routes (-> {::server/routes routes}
                        enable-graphql
                        enable-ide))))

;; taken from https://github.com/pedestal/pedestal/blob/50fe5ea89108998ac5a79a02a44432fd111ea6f8/samples/json-api/src/json_api/server.clj#L11
(defn run-dev
  "runs a service for the terminal"
  []
  (-> service
      enable-ide
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

@server

(start)
(stop)

((::server/routes @server))


(defonce runnable-service (http/create-server service))


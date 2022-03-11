(ns dtorter.http
  (:require [io.pedestal.http :as server]
            [io.pedestal.http.route :as route]
            [io.pedestal.test :as test]
            [com.walmartlabs.lacinia.pedestal :as lp]
            [dtorter.api :as api]))

(def service
  {:env :prod
   ::server/type :jetty
   ::server/port 8080
   })


;; taken from https://github.com/pedestal/pedestal/blob/50fe5ea89108998ac5a79a02a44432fd111ea6f8/samples/json-api/src/json_api/server.clj#L11
(defn run-dev
  "runs a service for the terminal"
  []
  (-> service
      (merge {:env :dev
              ::server/join? false
              ::server/routes #(route/expand-routes (deref #'routes))
              ::server/allowed-origin {:creds true :allowed-origins (constantly true)}})
      server/default-interceptors
      server/dev-interceptors
      server/create-server
      server/start))

(def schema (api/load-schema))
(def server (atom nil))

(defn start-server [_]
  (-> schema
      (lp/service-map {:graphiql true})
      server/create-server
      server/start))

(defn stop-server [server] (server/stop server))

(defn start [] (swap! server start-server))
(defn stop [] (swap! server stop-server))

(start)

@server


(defonce runnable-service (http/create-server service))

